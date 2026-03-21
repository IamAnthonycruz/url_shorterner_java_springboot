# URL Shortener

A production-style URL shortening service built with Java and Spring Boot. Converts long URLs into short, shareable links using Base62-encoded database IDs and resolves them via HTTP 302 redirects. Supports user registration, cookie-based session authentication backed by Redis, and URL ownership scoping.

## Tech Stack

- **Java 17** + **Spring Boot**
- **PostgreSQL** — persistent storage for URLs and users
- **Redis** — cache-aside pattern for redirect resolution + server-side session store
- **Spring Data JPA** — repository abstraction over Postgres
- **Spring Data Redis** — cache layer with TTL-based eviction, session management via `StringRedisTemplate`
- **Spring Security** — `BCryptPasswordEncoder` for password hashing, `SecurityFilterChain` for endpoint protection
- **Hibernate Validator** — request validation at the API boundary
- **Docker Compose** — local development environment for Postgres and Redis
- **Lombok** — boilerplate reduction

## How It Works

### URL Shortening

When a user submits a long URL, the service persists it to Postgres, generates a unique short code by Base62-encoding the auto-incremented database ID, and returns the shortened link. When someone visits the short link, the service checks Redis first (cache hit returns in ~1–2ms), falls back to Postgres on a cache miss, populates the cache for next time, and issues an HTTP 302 redirect to the original URL. Every redirect atomically increments a hit counter via a `@Modifying` JPQL query.

### Short Code Generation

Uses Base62 encoding (0-9, a-z, A-Z) of the database-generated primary key. This guarantees uniqueness without collision checks — each sequential ID maps to exactly one short code. The tradeoff is predictability of sequential codes, which is a known limitation documented for this learning project.

### Caching Strategy

Implements the **cache-aside** (lazy-loading) pattern:

1. Check Redis for the short code
2. On **cache hit** → return the long URL immediately, skip DB entirely
3. On **cache miss** → query Postgres, populate Redis with a 1-hour TTL, return the long URL

Cache invalidation on URL deletion updates the database first, then evicts the Redis key — creating a self-healing, TTL-bounded inconsistency window rather than a silent permanent failure if the eviction step fails.

### Authentication & Sessions

Cookie-based session authentication with Redis as the session store:

1. User registers with a username and BCrypt-hashed password
2. On login, the server verifies credentials, generates a UUID session ID, stores it in Redis with a 24-hour TTL, and returns it via a `Set-Cookie` header
3. The session cookie is configured with `HttpOnly` (XSS protection), `Secure` (HTTPS only), `SameSite=Lax` (CSRF protection while allowing redirect navigations), and `Max-Age` synced to the Redis TTL
4. On logout, the server deletes the session from Redis and kills the browser cookie with `Max-Age=0`

Session fixation is prevented by deleting any existing session before generating a new one at login time. The session ID is never exposed in response bodies — `HttpOnly` cookies are the sole transport mechanism.

## API

### Authentication

#### Register

```
POST /api/v1/auth/register
Content-Type: application/json

{
  "userName": "carlos",
  "password": "securepassword123"
}
```

**Response** — `201 Created`

Returns 409 Conflict if the username is already taken.

#### Login

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "userName": "carlos",
  "password": "securepassword123"
}
```

**Response** — `200 OK` with `Set-Cookie: session-id=<uuid>` header
```json
{
  "userName": "carlos"
}
```

Returns 401 Unauthorized for invalid credentials. Error messages do not reveal whether the username or password was incorrect.

#### Logout

```
POST /api/v1/auth/logout
Cookie: session-id=<uuid>
```

**Response** — `204 No Content` with expired `Set-Cookie` header to clear the browser cookie.

### URL Shortening

#### Shorten a URL

```
POST /api/v1/short-url
Content-Type: application/json

{
  "longUrl": "https://www.example.com/some/really/long/path"
}
```

**Response** — `201 Created`
```json
{
  "shortUrl": "http://localhost:8080/uwu/cb",
  "longUrl": "https://www.example.com/some/really/long/path",
  "createdAt": "2026-03-01T12:00:00"
}
```

#### Redirect

```
GET /uwu/{shortCode}
```

**Response** — `302 Found` with `Location` header pointing to the original URL. Atomically increments the hit counter. Soft-deleted URLs return 404.

## Project Structure

```
com.cruz.url_shortener
├── config/
│   ├── AppProperties.java              # @ConfigurationProperties for app config
│   └── SecurityConfig.java             # SecurityFilterChain, BCryptPasswordEncoder bean
├── component/
│   └── Base62Encoder.java              # Encode/decode between Long IDs and short codes
├── controller/
│   ├── AuthController.java             # Registration, login, logout endpoints
│   └── UrlController.java              # URL shortening and redirect endpoints
├── dto/
│   ├── LoginRequestDto.java            # Login credentials (username + password)
│   ├── LoginResponseDto.java           # Login response (username only, no session ID)
│   ├── RegistrationRequestDto.java     # Registration with validation constraints
│   ├── UrlRequestDto.java              # Inbound URL request with @NotBlank, @URL validation
│   └── UrlResponseDto.java             # Outbound URL response
├── entity/
│   ├── Url.java                        # JPA entity with @ManyToOne to User, soft delete flag
│   └── User.java                       # JPA entity (BIGINT PK, BCrypt-hashed password)
├── exception/
│   ├── GlobalExceptionHandler.java     # @RestControllerAdvice for consistent error responses
│   └── InvalidCredentialException.java # Auth-specific exception
├── mapper/
│   └── UrlMapper.java                  # Entity ↔ DTO conversion
├── repository/
│   ├── UrlRepository.java              # Spring Data JPA + @Modifying atomic hit counter
│   └── UserRepository.java             # User lookup by username
├── service/
│   ├── AuthService.java                # Auth service interface
│   ├── UrlService.java                 # URL service interface
│   └── impl/
│       ├── AuthServiceImpl.java        # Credential verification, Redis session management
│       └── UrlServiceImpl.java         # URL business logic, cache-aside orchestration
└── UrlShortenerApplication.java
```

## Design Decisions

- **Base62 over hashing** — Encoding the auto-increment ID avoids collision handling entirely. Hash-based approaches (MD5/SHA prefix) require retry logic on conflicts.
- **Two-write pattern** — The entity is saved once to get the DB-generated ID, then updated with the computed short code. Acceptable tradeoff for a non-bulk-insert workload.
- **302 over 301** — Temporary redirects let the server re-evaluate on each request. Permanent redirects (301) get cached by browsers indefinitely, breaking the ability to update or delete short links.
- **Soft delete over hard delete** — Hard-deleting URLs breaks existing shared/bookmarked links. Soft delete with an `isShortCodeActive` flag preserves link integrity and returns 404 for deactivated codes.
- **DB-first cache invalidation** — Update the database first, then evict Redis. If the eviction fails, the stale cache entry self-heals when the TTL expires. The reverse ordering risks a permanent inconsistency if the DB write fails after cache eviction.
- **Atomic hit counter** — Uses `@Modifying` JPQL query with `@Transactional` at the repository level to increment hit counts without read-modify-write race conditions.
- **DTO separation from entities** — Request/response DTOs prevent clients from setting internal fields like `id`, `hitCount`, or `createdAt`. The mapper layer keeps conversion logic centralized.
- **`@ConfigurationProperties` over `@Value`** — Centralizes app configuration in a single typed class rather than scattering `@Value` annotations across the codebase.
- **BIGINT over UUID for user PK** — User ID is purely internal and never exposed in API responses. BIGINT avoids the storage and indexing overhead of UUIDs when their uniqueness guarantees aren't needed.
- **Session ID excluded from response body** — Returning the session ID in JSON defeats `HttpOnly` cookie protection. The cookie is the sole transport for the session token.
- **BCrypt for password hashing** — One-way hashing with automatic salting. Intentionally slow to make brute-force attacks impractical. Each hash includes a unique salt, so identical passwords produce different hashes.
- **Login DTO omits password length validation** — Registration enforces minimum password length, but login does not. If the password policy is tightened later, users who registered under the old policy can still log in.
- **`/uwu/{shortCode}` redirect path** — Keeps redirect URLs short and memorable while avoiding collisions with the versioned `/api/v1/` prefix used by the REST endpoints.

## Running Locally

### Prerequisites

- Java 17+
- Docker & Docker Compose (for Postgres and Redis)

### Configuration

Set the following in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.jpa.hibernate.ddl-auto=update
app.base-url=http://localhost:8080
```

### Run

```bash
docker-compose up -d
./mvnw spring-boot:run
```

## Status

Active learning project following a structured backend engineering roadmap. Currently implemented through Part 5.2: core REST API, database layer, Base62 encoding, Redis caching, soft delete, atomic hit counting, user registration, and cookie-based session authentication. Next up: Spring Security filter chain (5.3) and URL ownership scoping (5.4).
```

## Status

This is an active learning project following a structured software engineering roadmap. Currently implemented: core REST API, database layer, Base62 encoding, caching, and redirect flow. Exception handling and SOAP integration are next on the build order.
