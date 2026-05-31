# Changelog

All notable changes to hrva.cc — the URL shortener.

---

## [v2.3] — 2026-05-31

### ✨ Added
- **Email verification** — new registrations must confirm their email before login; resend supported. Existing users grandfathered as verified; Google accounts auto-verified.
- **Two-factor authentication (TOTP)** — enroll an authenticator app (QR), confirm a code, receive one-time recovery codes. Login gains a second step. Local accounts only.
- **Account self-service** — delete your account (password-confirmed) along with all your URLs and API keys; download all your data as JSON (GDPR).
- **Password-protected links** — lock a short URL behind a password (registered users); visitors enter it before redirect.
- **URL tags** — tag links, filter by tag, manage tags on create/edit.
- **Click analytics** — per-URL and overview analytics (visits, unique recent visitors, 30-day new-visitor trend).
- **Customizable QR codes** — choose colors, size, and error-correction level; download as PNG or SVG.
- **Admin signup email + stats** — admin notified on new registrations; dashboard shows new-user counts (7d / 30d).

### 🔒 Security
- `active` flag now enforced at login (password + OAuth) and per-request; deactivating a user revokes their API keys.
- API keys are valid only while both the key and its owner are active.
- Admin-only endpoints hidden from the public API docs.

### 🎨 Improved
- URL and API-key status now reflects the real reason (Active / Expired / Limit reached / Revoked / Deactivated / Blocked) instead of a generic "Revoked".
- Fixed Google sign-in requiring two attempts; fixed the admin user deactivate-toggle 500.

### 🧪 Testing
- Backend test suite expanded to cover verification, 2FA, analytics, tags, self-delete, and status logic.

---

## [v2.1] — 2026-05-17

### ✨ Added
- **URL & User search/filtering** — Dashboard and admin pages now have search inputs, status filters (Active/Inactive/All), "Expired only" checkbox, and date range filters. Backend uses JPA Specifications for dynamic queries.
- **Pagination improvements** — "Showing X–Y of Z" count is always visible along with page size selector and prev/next buttons, even on single-page results.
- **Status page link** — Added `status.hrva.cc` link back to the footer (was lost in the rewrite).
- **More test coverage** — 6 new unit tests for URL and User filtering logic (now 146 total, all passing).

### 🎨 Improved UX
- **Footer now sticks to bottom** — Fixed double scrollbar issue. Footer stays at the bottom regardless of content height.
- **Logged-in shorten form** — Custom alias, visit limit, and expiration date are always visible (no need to click "Show advanced options").
- **Public landing page** — Removed advanced options that silently didn't work for anonymous users. Added subtle upsell ("Sign up free for custom aliases & analytics") in the hero text and after shortening a link.
- **Deactivate toggle icon** — Changed from `ToggleLeft` (ambiguous) to `ToggleRight` (clearly shows the URL is active and can be turned off).
- **URL count accuracy** — Fixed stats counter to use `totalElements` from API instead of page content length.

### 🔧 Changed
- Refactored backend filter params into `UrlSearchDto` / `UserSearchDto` instead of scattered `@RequestParam` annotations.

### 🧪 Testing
- Backend: 146 unit tests (JUnit 5 + Mockito), all passing.
- Frontend: Build clean with no errors.

---

## [v2.0] — React Rewrite

Complete rewrite of the frontend from Angular to React (Next.js). Full feature parity plus new capabilities.

### ✨ Features
- **URL shortening** — Create short links with optional custom alias, visit limit, and expiration date.
- **Anonymous shortening** — Shorten URLs without an account.
- **User accounts** — Register, login, password reset, JWT-based auth.
- **Dashboard** — View, search, sort, and manage all your URLs in one place.
- **URL analytics** — Per-link visit tracking, QR codes, link previews.
- **API keys** — Generate and manage API keys for programmatic access.
- **Bulk URL creation** — Create multiple URLs at once.
- **CSV export** — Download your URLs as a CSV file.
- **Admin panel** — User management (edit, activate/deactivate, delete), system stats.
- **OAuth2 login** — Sign in with Google or GitHub.
- **Dark/light theme** — System-aware theme with manual toggle.
- **Responsive design** — Full mobile support.

### 🛡️ Security
- Google Safe Browsing integration — every URL is checked before shortening.
- JWT stateless auth with configurable expiry.
- Rate limiting on login attempts.
- Account deactivation after inactivity.

### ⚡ Performance
- Redis caching for URL lookups with graceful degradation.
- Read-only transactions on service layer.
- Optimized Docker builds with multi-arch support.

### 🧪 Testing
- 140+ unit tests covering controllers, services, validators, converters, and models.

---

## [v1.0] — Angular Frontend + Spring Boot Backend

Initial release of hrva.cc.

### ✨ Features
- URL shortening with custom aliases
- Visit limits and expiration dates
- Link analytics dashboard
- API key management
- User authentication (email/password)
- Password reset flow
- QR code generation
- Admin user management
- Docker deployment
- Swagger API documentation

---

## v0.x — Prototype & Development

Early development phase with Spring Boot backend and Angular frontend. Core functionality established:
- URL CRUD operations
- Visit tracking
- User registration and authentication
- Basic admin capabilities

---

*hrva.cc — Short links, shaped by you.*
