# Changelog

All notable changes to hrva.cc — the URL shortener.

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
