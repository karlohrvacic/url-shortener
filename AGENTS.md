# URL-Shortener Agent Guide

## Build & Run
- **Java 25**, **Spring Boot 4.0.6**, **Maven**. No Gradle artifacts.
- `.mvn/wrapper/` exists but `mvnw` is missing — use system `mvn`.
- Entry point: `cc.hrva.urlshortener.UrlShortenerApplication`
- Local dev:
  ```bash
  # Start backing services first
  docker compose -f docker/docker-compose.yml up
  # Then run the app with local profile
  mvn spring-boot:run -Dspring-boot.run.profiles=local
  ```
- Build: `mvn clean package` → output in `target/`
- Docker: `Dockerfile` copies `target/*.jar`, multi-arch CI build

## Local Dev Users
`LocalDataInitializer` (only with `local` profile) bootstraps two users:

| Email | Password | Role | Slots |
|-------|----------|------|-------|
| `admin@localhost` | `admin123` | ROLE_ADMIN | 10 |
| `user@localhost` | `user123` | ROLE_USER | 4 |

Idempotent — safe on restart. Liquibase seeds `ROLE_USER` and `ROLE_ADMIN` authorities.

## Local Dependencies
- **Postgres** (`localhost:5432`, `root:root`, db `url_shortener`)
- **Redis** (`localhost:6379` — overridden in `application-local.yaml`, no longer needed to set manually)
- **MailHog** (`localhost:8025`, SMTP on `localhost:1025`)

## Testing
- `mvn test` — 140 tests, JUnit 5 + Mockito + AssertJ
- No `@ActiveProfiles`, no Testcontainers, no test slicing — all plain `@ExtendWith(MockitoExtension.class)` unit tests
- `src/test/` mirrors `src/main/` package structure

## Package Structure
```
cc.hrva.urlshortener
├── client/          # GoogleSafeBrowsingApi (external API client)
├── configuration/   # Beans, props, cache, security, web config, exception handler
├── controller/      # REST controllers
├── converter/       # DTO↔entity converters (Spring Converter interface)
├── dto/             # Request/response DTOs (records and classes)
├── exception/       # Standard-named exceptions (*NotFoundException, Invalid*Exception)
├── listener/        # Spring event listeners (auth success/failure)
├── model/           # JPA entities, codebook, enums
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, token provider, auth entry point, ClientIpResolver
├── service/         # Interfaces + impl/ implementations
└── validator/       # Interfaces + impl/ validators
```
**No `beans/` package** — contents split into `security/`, `client/`, `configuration/`.

## API Patterns
- **Plural resource names**: `/api/v1/urls`, `/api/v1/api-keys`, `/api/v1/users`, `/api/v1/auth`
- **Proper HTTP methods**: `POST` for create, `DELETE` for delete, `PATCH` for state changes, `GET` for reads
- **API keys in header**: `X-Api-Key` header (NOT in URL path)
- **Pagination**: `@PageableDefault(size=20)` on all list endpoints — pass `?page=0&size=20` query params
- **Response DTOs**: Controllers return `UrlResponse`, `ApiKeyResponse`, `UserDto` — NOT JPA entities directly
- **Error format**: `{"error": "message"}` via `GlobalExceptionHandler` (`@ControllerAdvice`)
- **Exception → HTTP status mapping**: `UserNotFoundException`/`ApiKeyNotFoundException` → 404, `EmailExistsException`/`ShortUrlAlreadyExistsException` → 409, `NoAuthorizationException` → 403, `BadCredentialsException` → 401, `CommonException` (catch-all for all custom exceptions not handled above) → 400, everything else → 500.
- **Validation errors**: Return `{"field": "message"}` map on 400.

## Key Endpoints
| Method | Path | Auth | Notes |
|--------|------|------|-------|
| POST | `/api/v1/urls` | Optional `X-Api-Key` | Create short URL |
| GET | `/api/v1/urls/{short}` | None | Redirect info (records visit) |
| GET | `/api/v1/urls/{short}/peek` | None | Quick peek (longUrl, shortUrl, createDate) |
| GET | `/api/v1/urls/{short}/preview` | None | Rich preview (title, description, og:image) |
| GET | `/api/v1/urls/{short}/qr` | None | QR code PNG (`?size=300`) |
| POST | `/api/v1/urls/bulk` | ROLE_USER | Bulk create (JSON array) |
| PUT | `/api/v1/urls/{id}` | Required | Update URL |
| PATCH | `/api/v1/urls/{id}/deactivate` | Required | Deactivate |
| DELETE | `/api/v1/urls/{id}` | Required | Delete |
| POST | `/api/v1/api-keys` | ROLE_USER | Generate key |
| PATCH | `/api/v1/api-keys/{id}/revoke` | Required | Revoke key |
| POST | `/api/v1/auth/register` | None | Register |
| POST | `/api/v1/auth/login` | None | Login (returns JWT) |
| POST | `/api/v1/auth/password-reset` | None | Request reset |
| POST | `/api/v1/auth/password-reset/confirm` | None | Confirm reset |
| GET | `/api/v1/admin/stats` | ROLE_ADMIN | Dashboard stats (users, URLs, cache, etc.) |
| GET | `/actuator/health` | Public | Health check |
| GET | `/actuator/**` | ROLE_ADMIN | Metrics, caches, env, beans |

## SSO (OAuth2)
- Google and GitHub OAuth2 login supported via `spring-boot-starter-oauth2-client`.
- **Login flow**: Frontend links to `/oauth2/authorization/google` (or `/github`). Backend handles callback, creates/finds user by email, issues JWT, redirects to `{frontendUrl}/auth/callback?token=...`.
- **OAuth2 users** get `authProvider = "google"` or `"github"` on the User record. Their email cannot be changed (blocked at service level).
- Configured via `application-oauth.yaml` (gitignored) or environment variables for production.
- OAuth2 client secrets for local dev go in `application-oauth.yaml` (excluded from git via `.gitignore`).

## Database
- **Liquibase** root: `src/main/resources/db/dbchangelog.xml`
- Migrations auto-run on startup. Codebook seeds ROLE_USER(id=1) and ROLE_ADMIN(id=2).
- User accounts have `AUTH_PROVIDER` column — `"local"` for email/password users, `"google"`/`"github"` for SSO.

## Performance
- **Redis caching**: `@Cacheable("urls")` on URL lookup (`peekUrlByShortUrl` only). 60-min TTL. `@CacheEvict` on mutations.
- **Graceful degradation**: `CacheErrorHandler` logs and falls through to DB when Redis is down — no 500s.
- **Read-only transactions**: `@Transactional(readOnly = true)` at class level on all services, overridden with `@Transactional` on write methods.
- **SafeBrowsing client**: Singleton bean (not built per-request). Graceful degradation on API error (returns `List.of()`).

## Security
- JWT in `Authorization: Bearer` header. Stateless sessions.
- Login rate limiting: Guava cache, 20 attempts/day per IP, 1-day expiry.
- CORS via `CorsConfigurationSource` bean (not headers in JwtFilter). Allowed: frontend origin + all standard methods.
- Client IP resolved via `ClientIpResolver` + `ForwardedHeaderFilter` (never read X-Forwarded-For manually).
- Logging: SLF4J via `@Slf4j`, parameterized `{}` format.
- Actuator endpoints: `/actuator/health` public, all others ROLE_ADMIN only.

## Scheduled Tasks
- **URL expiry**: Daily at 8 AM, sends email notification for URLs expiring within 24 hours (configurable via `app.url-expiration-notification-hours`).
- **IP cleanup**: Every minute, deactivates and deletes old IP address records.
- **Reset tokens**: Every minute, deactivates and deletes expired password reset tokens.
- **URL deactivation**: Every minute, deactivates expired URLs.
- **API key deactivation**: Every hour, deactivates expired API keys.
- **User deactivation**: Daily at 10 AM, deactivates unused accounts after configured inactivity period.

## Config Profiles
- **`local`**: `localhost` Postgres/Redis/MailHog, hardcoded JWT secret, dev users auto-created, verbose SQL logging. Auto-imports `application-oauth.yaml` for SSO secrets.
- **`prod`**: Remote Postgres, no dev defaults. Secrets injected externally.
- **`oauth`**: Optional profile holding OAuth2 client credentials. Imported by `local` profile via `spring.config.import`.
- Default: `application.yaml` sets base values (cache TTL, URL length, API limits, etc.).
- Tests run without any profile — application context is not loaded.

## CI / Deploy
- `.github/workflows/build.yml`: `mvn clean package` → Docker build/push to `ghcr.io`
- Multi-arch: `linux/amd64,linux/arm64`
- Pushes on `main` branch. PRs only build (no push).
- Portainer env vars: `OAUTH2_GOOGLE_CLIENT_ID`, `OAUTH2_GOOGLE_CLIENT_SECRET`, `OAUTH2_GITHUB_CLIENT_ID`, `OAUTH2_GITHUB_CLIENT_SECRET`.
