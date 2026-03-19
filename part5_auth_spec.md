## Part 5: Session-Based Authentication
**Time:** 4–5 hours  
**Goal:** Add user registration, login, and session management so URLs are owned by the users who created them — using Redis as the session store.

*Everything works anonymously at this point. This part adds identity to the system — users own their URLs, and only authenticated users can create or delete them. You're not rewriting your existing logic, you're wrapping it with an auth layer.*

### Subproblem 5.1 — User Entity & Registration Endpoint

#### Logic Walkthrough
You need a second table: `users`. At minimum it needs:
- `id` — auto-incrementing primary key (BIGINT)
- `username` — unique, indexed (VARCHAR(50))
- `password` — the hashed password (VARCHAR(255), NOT the plaintext)
- `created_at` — timestamp with time zone

Map this as a JPA entity the same way you did with `Url`. The `username` column needs a unique constraint — two users with the same username breaks everything downstream.

Now add a foreign key from `urls` to `users`. Your `Url` entity gets a new field: `@ManyToOne` with a `@JoinColumn(name = "user_id")` pointing to the `User` entity. This makes URLs owned — every shortened URL belongs to whoever created it.

For registration, build a `POST /api/v1/auth/register` endpoint. The flow:
1. Validate the request DTO (username not blank, password not blank, password meets minimum length)
2. Check if the username already exists — if it does, return 409 Conflict
3. Hash the password with BCrypt
4. Save the user entity
5. Return 201 Created

**Never store plaintext passwords.** Use Spring Security's `BCryptPasswordEncoder` — it handles salting automatically. Each hash includes a unique salt, so two users with the same password get different hashes. To use it, add `spring-boot-starter-security` to your `pom.xml` and register a `BCryptPasswordEncoder` `@Bean` in a config class.

**Gotcha:** The moment you add `spring-boot-starter-security` to your classpath, Spring Security auto-configures itself and locks down *every endpoint* with HTTP Basic auth and a generated password. Your existing endpoints will stop working. You need to define a `SecurityFilterChain` bean immediately (covered in 5.3) to override this — set it to `permitAll()` initially so your existing functionality doesn't break while you build the auth layer.

#### Reading Resource
[Baeldung — Spring Security Authentication](https://www.baeldung.com/spring-security-authentication-and-registration)

**YouTube Search:** `"Spring Boot BCrypt password hashing registration endpoint"`

#### Where You'll See This Again
1. Every SaaS backend starts with this exact pattern — a `users` table with hashed passwords and a registration endpoint.
2. Your chat app (Project 2) will reuse this same user entity and registration flow — the auth layer transfers directly.
3. OAuth2 providers (Google, GitHub login) still create a local user record when someone signs in for the first time — same table, different source of identity.

**Why this matters:** Password hashing is the single most important security decision in a user-facing backend. One leaked database with plaintext passwords ends careers. BCrypt is the industry default because it's intentionally slow — making brute-force attacks impractical.

---

### Subproblem 5.2 — Session Management with Redis

#### Logic Walkthrough
After a user logs in, you need to remember who they are across requests. HTTP is stateless — each request is independent. Sessions solve this by storing a token on the client (a cookie) that maps to server-side state (the session data).

Here's the flow:
1. User sends `POST /api/v1/auth/login` with username and password
2. Server verifies the password against the stored hash using `BCryptPasswordEncoder.matches()`
3. If valid: generate a session ID (a random UUID), store it in Redis with the user's ID as the value, set a TTL (e.g., 24 hours), and return the session ID in a `Set-Cookie` header
4. If invalid: return 401 Unauthorized

On every subsequent request, the browser sends the cookie back automatically. Your server reads the session ID from the cookie, looks it up in Redis, and now knows who the user is. This is the same cache lookup pattern you built in Part 4 — just pointing at session data instead of URLs.

You can implement this manually with `StringRedisTemplate`:
```
Login:  SET session:{uuid} userId EX 86400
Lookup: GET session:{uuid}
Logout: DEL session:{uuid}
```

Or use Spring Session with Redis, which automates the cookie handling and session lifecycle. For learning, start manual — you already know `StringRedisTemplate` from Part 4. The mental model is identical: Redis stores key-value pairs with TTLs, and you're just changing what the keys and values represent.

Add a `POST /api/v1/auth/logout` endpoint that deletes the session from Redis and clears the cookie. Without this, sessions linger until the TTL expires.

**Gotcha:** Session fixation — if you reuse a session ID after login that existed before login, an attacker who knows the pre-login session ID can hijack the post-login session. Always generate a *new* session ID at login time, never reuse an existing one.

#### Reading Resource
[OWASP — Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)

**YouTube Search:** `"session based authentication Redis Spring Boot cookie"`

#### Where You'll See This Again
1. Redis-backed sessions are the standard for any horizontally scaled web app — this is exactly how e-commerce sites, dashboards, and SaaS products manage auth state across multiple server instances.
2. JWT (JSON Web Tokens) are the stateless alternative — instead of storing state in Redis, you encode it in the token itself. Understanding sessions first makes JWTs click faster because you'll see what problem they're solving differently.
3. Your existing cache-aside pattern for URL lookups is structurally identical to session lookups — Redis key → value with TTL. Same tool, different data.

**Why this matters:** Session management is where most authentication vulnerabilities live — session fixation, session hijacking, missing expiration. Building it manually once teaches you what Spring Session and other frameworks abstract away, so you can reason about security properties rather than trusting magic.

---

### Subproblem 5.3 — Protecting Endpoints with a Security Filter

#### Logic Walkthrough
Now that sessions exist, you need to enforce them. Some endpoints should require authentication (creating and deleting URLs), and some should remain public (redirects, registration, login).

Spring Security processes every request through a filter chain before it reaches your controller. You configure this chain by defining a `SecurityFilterChain` bean. But here's the key insight: instead of using Spring Security's built-in session mechanism and form login, you're writing a **custom filter** that reads your session cookie and sets the security context manually. This gives you full control.

Create a filter (extending `OncePerRequestFilter`) that:
1. Reads the session cookie from the request
2. If no cookie → do nothing, let the request continue (it'll be treated as unauthenticated)
3. If cookie exists → look up the session ID in Redis
4. If Redis has the session → create a `UsernamePasswordAuthenticationToken` and set it on the `SecurityContextHolder`
5. If Redis doesn't have the session (expired/invalid) → do nothing

Then in your `SecurityFilterChain` config:
- `POST /api/v1/auth/**` → `permitAll()` (registration and login)
- `GET /uwu/**` → `permitAll()` (redirects are public)
- `POST /api/v1/short-url` → `authenticated()`
- `DELETE /api/v1/short-url/**` → `authenticated()`
- Disable CSRF (you're not using server-rendered forms)
- Set session creation policy to `STATELESS` (you're managing sessions yourself, not Spring)

Register your custom filter to run before `UsernamePasswordAuthenticationFilter` in the chain — this ensures your session resolution happens before Spring tries its own auth.

**Gotcha:** `SecurityContextHolder` is thread-local by default. In a servlet container (Tomcat), each request gets its own thread, so setting the authentication on one request doesn't bleed into another. But if you ever use async processing or reactive Spring, this model breaks. Note it for now.

#### Reading Resource
[Baeldung — Spring Security Filter Chain](https://www.baeldung.com/spring-security-custom-filter)

**YouTube Search:** `"Spring Security custom filter OncePerRequestFilter tutorial"`

#### Where You'll See This Again
1. Every Spring Security integration — OAuth2, JWT, API key auth — works by adding a filter to this chain. Once you understand the filter model, every auth strategy is just a different filter.
2. Your reverse proxy project (Part 2 roadmap) implements this same concept at the TCP level — inspect the request, decide whether to forward it, potentially modify it. Same pattern, lower abstraction.
3. API gateways (Kong, Zuul) use filter chains to enforce rate limiting, auth, and request transformation — the architecture is identical to Spring's filter chain.

**Why this matters:** Spring Security is one of the most powerful and most confusing parts of the Spring ecosystem. Most developers cargo-cult configurations without understanding the filter chain. Building a custom filter first gives you the mental model that makes the magic make sense.

---

### Subproblem 5.4 — Scoping URLs to Users

#### Logic Walkthrough
With auth in place, two things change in your existing code:

**1. URL creation now includes the owner.**  
In your `shortenUrl` service method, you need the authenticated user's ID. Get it from the `SecurityContextHolder` or pass it down from the controller. When saving the `Url` entity, set the `user` field. The duplicate URL check also changes — the same long URL submitted by two different users should create two separate short codes. Update your query from `findByLongUrl(url)` to `findByLongUrlAndUser(url, user)`.

**2. URL deletion requires ownership verification.**  
When `DELETE /api/v1/short-url/{shortCode}` is called, look up the URL, check that the authenticated user's ID matches the URL's owner. If it doesn't, return 403 Forbidden — not 404. Returning 404 for "exists but you don't own it" is a design choice (it hides the existence of the resource), but 403 is more honest. Pick one and be consistent.

For the redirect endpoint — keep it public. Anyone with the short link can use it. The auth only gates creation and deletion.

You'll also want a `GET /api/v1/short-url/mine` endpoint that returns all URLs created by the authenticated user, with hit counts. This is the first time you'll write a query that filters by a foreign key relationship — `findByUserId(Long userId)` in your repository.

**Gotcha:** When you add the `User` association to `Url`, JPA's `ddl-auto=update` will add the `user_id` column but it'll be nullable (since existing rows don't have a user). For development this is fine. In production you'd run a migration to backfill or clean up orphaned rows before adding a NOT NULL constraint.

#### Reading Resource
[Baeldung — Spring Data JPA Relationships](https://www.baeldung.com/jpa-many-to-many)

**YouTube Search:** `"Spring Security get authenticated user in service layer"`

#### Where You'll See This Again
1. Multi-tenant SaaS applications do this at scale — every query is scoped by `tenant_id` or `user_id`. The pattern you're building here (ownership check on every mutation) is the foundation of data isolation.
2. In your chat app (Project 2), messages will be scoped to conversations, and conversations to users — same foreign key ownership pattern.
3. Authorization decisions (can this user do this action on this resource?) are the core of RBAC (Role-Based Access Control) and ABAC (Attribute-Based Access Control) systems used in enterprise software.

**Why this matters:** Authentication tells you *who* someone is. Authorization tells you *what they can do*. Most security bugs aren't in the login flow — they're in the "I forgot to check ownership before deleting" path. Building that discipline here means it's automatic in future projects.

---

## Key Concepts to Pause On

These are worth the extra time to understand deeply — they'll show up in interviews and in the Go projects:

- **Cache-aside vs write-through:** Why you chose cache-aside and what write-through would look like
- **301 vs 302 redirects:** Browser caching behavior and why it matters for short links
- **Base62 encoding:** The math, and why sequential IDs make codes predictable
- **Session vs JWT:** Why you chose server-side sessions, and what stateless tokens trade off (no server-side revocation, token size, no Redis dependency)
- **Index design:** Why `short_code` needs an index and what a query plan looks like without it (run `EXPLAIN ANALYZE` in psql to see the difference)
- **Password hashing:** Why BCrypt over SHA-256, what salting prevents, and why hashing speed is a security property
- **The filter chain model:** How Spring Security processes requests before they reach your controller, and why every auth strategy is just a different filter
