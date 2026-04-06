# URL Shortener

A production-style URL shortening service built with Java and Spring Boot. Supports user authentication, Redis caching, soft delete with reactivation, and a full integration test suite powered by Testcontainers.

## Tech Stack

- **Java 17** / **Spring Boot 4.0**
- **PostgreSQL** — persistent storage with indexed lookups
- **Redis** — cache-aside layer for redirect performance and session storage
- **Spring Security** — custom cookie-based session authentication
- **Docker Compose** — local development environment
- **Testcontainers** — integration tests against real Postgres and Redis
- **JUnit 5 / Mockito** — unit and integration testing

## Features

**URL Shortening:** Submit a long URL, receive a Base62-encoded short code derived from the database auto-increment ID. Redirect via `GET /{shortCode}` with HTTP 302. Hit counts are atomically incremented on every redirect.

**Cache-Aside Caching:** Redirect lookups check Redis first. On a cache miss, the long URL is fetched from Postgres and written to Redis with a 1-hour TTL. Cache entries are evicted on soft delete, and TTL provides self-healing for any residual inconsistency.

**Soft Delete & Reactivation:** URLs are deactivated rather than hard-deleted, preserving short code uniqueness. If the same user re-submits a previously deactivated URL, the existing short code is reactivated instead of creating a duplicate.

**Session-Based Authentication:** Users register with BCrypt-hashed passwords. Login produces a session UUID stored in Redis with a 24-hour TTL, returned as an `HttpOnly`, `Secure`, `SameSite=Lax` cookie. A custom `OncePerRequestFilter` reads the cookie, resolves the session from Redis, and sets the Spring Security context.

**User Ownership Scoping:** Every URL is associated with the user who created it. Duplicate URL detection is scoped per user. Deletion requires ownership verification. Users can view all their URLs via `GET /api/v1/short-urls/mine`.

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | No | Register a new user |
| POST | `/api/v1/auth/login` | No | Login, receive session cookie |
| POST | `/api/v1/auth/logout` | No | Logout, clear session |
| POST | `/api/v1/short-urls` | Yes | Shorten a URL |
| GET | `/{shortCode}` | No | Redirect to the original URL |
| GET | `/api/v1/short-urls/mine` | Yes | List all URLs for the authenticated user |
| DELETE | `/api/v1/short-urls/{shortCode}` | Yes | Soft-delete a URL (owner only) |

## Design Decisions

**Base62 over hashing for short codes.** Hashing requires collision detection and retry logic. Base62 encoding of the auto-increment ID is mathematically guaranteed unique, requires no collision checks, and produces short, predictable-length codes. Tradeoff: sequential IDs make codes enumerable — noted as a known limitation.

**Two-write pattern for short code generation.** The short code depends on the database-generated ID, so the entity is saved first with a null short code, then updated with the computed code. This avoids the complexity of a separate ID sequence.

**Cache-aside over write-through.** The application manages the cache explicitly rather than relying on automatic cache population. This keeps the caching logic visible and testable, and avoids caching URLs that are never accessed.

**Database-first cache invalidation.** On soft delete, the database is updated before the cache is evicted. If the eviction fails, the TTL provides a bounded window of inconsistency that self-heals — preferable to a permanent silent failure from the reverse order.

**Soft delete over hard delete.** Hard-deleting a URL breaks existing shared or bookmarked links with no recovery path. Soft delete with an `isShortCodeActive` flag preserves the record and allows reactivation, while also avoiding unique constraint conflicts on `long_url` per user.

**Manual Redis sessions over Spring Session.** Building session management with `StringRedisTemplate` directly — `SET session:{uuid} userId EX 86400` — makes the session lifecycle explicit and uses the same caching patterns already established in the project.

**Custom security filter over Spring's built-in auth.** A `OncePerRequestFilter` that reads the session cookie, resolves the user from Redis, and sets the `SecurityContextHolder` keeps the auth mechanism transparent. The service layer receives a plain `Long` userId, staying framework-agnostic and testable.

**`@Modifying` for atomic hit counting.** A JPQL `UPDATE` query increments the hit count directly in the database, avoiding the read-modify-write race condition of loading the entity, incrementing in Java, and saving back.

**302 over 301 for redirects.** 301 (permanent) causes browsers to cache the redirect indefinitely. If a short code is ever deactivated or reassigned, cached 301s cannot be cleared. 302 (temporary) ensures every click reaches the server.

## Testing Strategy

Tests are organized by layer, following the test pyramid: many fast unit tests, fewer integration tests against real infrastructure.

**Unit Tests — Base62 Encoder:** Pure JUnit 5 tests covering known conversions, edge cases, and determinism. No Spring context, no dependencies — runs in milliseconds.

**Unit Tests — Service Layer (Mockito):** `UrlServiceImpl.getLongUrl()` tested with mocked `StringRedisTemplate` and `UrlRepository`. Verifies cache-hit path returns from Redis without touching the database, and cache-miss path fetches from Postgres and populates Redis.

**Repository Integration Tests (`@DataJpaTest` + Testcontainers):** Tests run against real PostgreSQL via Testcontainers. Covers unique constraint enforcement on `short_code` and `username`, custom query correctness for `findByShortCode`, atomic `incrementHitCount` with persistence context clearing, and `@ManyToOne` relationship traversal via `findAllByUserId`.

**Controller Integration Tests (`@SpringBootTest` + MockMvc):** Full integration tests from HTTP through the entire stack. Covers authenticated URL creation (201), unauthenticated access rejection, redirect with Location header verification (302), short code not found (404), user registration (201), and duplicate username rejection (409).

**Cache Behavior Tests (`@SpringBootTest` + Real Redis):** Verifies cache-aside correctness against real Redis via Testcontainers. Covers cache population on first redirect (key existence, value correctness, TTL), cache invalidation on soft delete (key removal, subsequent 404), and DB bypass on cache hit using `@SpyBean` on the repository.

**Infrastructure:** All integration tests extend a shared `BaseIntegrationTest` using the singleton container pattern — static PostgreSQL and Redis containers started once per test suite via a static initializer block, with `@DynamicPropertySource` injecting runtime connection details.

## Running Locally

**Prerequisites:** Java 17+, Maven, Docker

```bash
# Start Postgres and Redis
docker compose up -d

# Run the application
./mvnw spring-boot:run

# Run tests (requires Docker for Testcontainers)
./mvnw test
```

## Project Structure

```
src/main/java/com/cruz/url_shortener/
├── component/          Base62Encoder
├── config/             SecurityConfig, AppProperties
├── controller/         AuthController, UrlController
├── dto/                Request/Response DTOs
├── entity/             Url, User (JPA entities)
├── exception/          ShortCodeNotFoundException, RestrictedAccessException
├── filter/             CookieAuthHandler
├── mapper/             UrlMapper
├── repository/         UrlRepository, UserRepository
└── service/            UrlService, AuthService

src/test/java/com/cruz/url_shortener/
├── BaseIntegrationTest.java
├── Base62EncoderTest.java
├── UrlServiceTest.java
├── UrlRepositoryTest.java
├── UrlControllerTest.java
├── AuthControllerTest.java
└── RedisTest.java
```
```

## Status

This is an active learning project following a structured software engineering roadmap. Currently implemented: core REST API, database layer, Base62 encoding, caching, and redirect flow. Exception handling and SOAP integration are next on the build order.
