# Project 1: URL Shortener
**Stack:** Java, Spring Boot, PostgreSQL, Redis, Spring-WS (SOAP)  
**Estimated Time:** 18–24 hours  
**Goal:** Build a production-style URL shortening service that touches REST API design, database modeling, caching strategy, JSON handling, and legacy SOAP integration.

> **Note:** This document is ordered by build dependency — each part depends only on what came before it. You can follow it top to bottom without jumping around.

---

## Part 1: Project Setup & Spring Boot Foundation
**Time:** 2–3 hours  
**Goal:** Get a running Spring Boot app with the right dependencies wired up before writing a single line of business logic.

### Subproblem 1.1 — Initializing the Project with the Right Dependencies

#### Logic Walkthrough
Go to [start.spring.io](https://start.spring.io) and generate a Maven project with Java. The dependency choices you make here determine what Spring auto-configures for you. Here's what you need and why:

- **Spring Web** — brings in the embedded Tomcat server and the MVC framework. Without this, you have no web server.
- **Spring Data JPA** — gives you the repository abstraction over your DB. Instead of writing raw SQL, you'll define an interface and Spring generates the implementation.
- **PostgreSQL Driver** — the JDBC driver so JPA can actually talk to Postgres.
- **Spring Data Redis** — wires up a `RedisTemplate` and `StringRedisTemplate` you'll use for caching. Also enables `@Cacheable` annotations if you want the declarative approach.
- **Spring Boot Starter Validation** — adds Hibernate Validator so you can put `@NotBlank`, `@URL` etc. on your DTOs.
- **Lombok** — optional but eliminates boilerplate getters/setters/constructors on your model classes.
- **Spring Web Services** — for the SOAP endpoint in Part 5. Add it now so you don't have to restart your setup.

Once generated, open `application.properties` and configure three things: your Postgres connection URL, your Redis host/port, and your server port. Spring Boot will fail fast on startup if it can't connect, which is actually useful — you want to catch misconfiguration early.

One non-obvious thing: Spring Boot's autoconfiguration scans your classpath and wires beans based on what it finds. If you add the Redis dependency but don't configure a Redis host, it defaults to `localhost:6379`. If nothing is running there, your app will still start but throw errors at runtime when the cache is first hit. Set it explicitly.

**Gotcha:** JPA's `spring.jpa.hibernate.ddl-auto` setting controls whether Hibernate creates/drops your schema on startup. Use `update` for development (it adds new columns but doesn't delete old ones) and `validate` for production. Never use `create-drop` unless you enjoy losing data.

#### Reading Resource
[Spring Boot Reference — Application Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)

**YouTube Search:** `"Spring Boot project structure explained for beginners"`

#### Where You'll See This Again
1. Every microservice in a Spring-based enterprise stack is bootstrapped this way — the dependency list in `pom.xml` is effectively the capability declaration for the service.
2. In cloud deployments (AWS, GCP), these same `application.properties` values get injected as environment variables rather than hardcoded — understanding the config layer matters for 12-factor app design.
3. Docker Compose for local dev uses the same host/port pattern — your `application.properties` will reference `redis` as a hostname when running in a container network.

**Why this matters:** Most bugs in backend projects aren't in the logic — they're in misconfiguration. Getting comfortable reading Spring's startup logs and understanding what it's doing during autoconfiguration saves hours of debugging later.

---

### Subproblem 1.2 — Understanding Spring's Request Lifecycle

#### Logic Walkthrough
Before you write a single controller, understand what happens between a request hitting your server and your code running. This mental model will explain every annotation you use.

When a request comes in, Spring's `DispatcherServlet` receives it. It looks at the URL and HTTP method, finds the `@Controller` or `@RestController` class whose `@RequestMapping` matches, and invokes the right method. The `@RestController` annotation is just `@Controller` + `@ResponseBody` combined — it tells Spring to serialize the return value directly to JSON instead of treating it as a view name.

The flow looks like this:
```
HTTP Request → DispatcherServlet → HandlerMapping (find the right method)
→ HandlerAdapter (invoke it) → Your method runs → Return value
→ HttpMessageConverter (serialize to JSON) → HTTP Response
```

`HttpMessageConverter` is the piece that turns your Java objects into JSON. Spring Boot auto-configures Jackson as the converter when it sees `spring-boot-starter-web` on the classpath. Jackson uses reflection to serialize your fields by default — it looks for getters or public fields. This is why Lombok's `@Data` or `@Getter` annotations matter: without them, Jackson can't read your fields and you get empty JSON.

**Gotcha:** If you return a plain `String` from a `@RestController` method, Spring returns it as `text/plain`, not JSON. If you want `{"message": "ok"}`, return an object, not a string.

#### Reading Resource
[Baeldung — Spring's DispatcherServlet](https://www.baeldung.com/spring-dispatcherservlet)

**YouTube Search:** `"Spring MVC request lifecycle flow diagram"`

#### Where You'll See This Again
1. Express.js in Node has the same concept — middleware chain processes requests before they reach your route handler.
2. Django's middleware stack works identically — each layer can inspect or modify the request before the view function runs.
3. When you build the reverse proxy in Part 2 of your roadmap, you'll be implementing this same pattern manually at the TCP level — understanding it abstractly here will make that click.

**Why this matters:** Every framework has a request lifecycle. Understanding Spring's early means you'll debug annotation issues by reasoning about the pipeline, not by googling error messages.

---

## Part 2: Database Design & Short Code Generation
**Time:** 3–4 hours  
**Goal:** Model the data correctly, wire up JPA, and implement a collision-safe short code generation strategy.

*You build the data layer before the API layer because the controller and service depend on the entity and repository — not the other way around.*

### Subproblem 2.1 — Schema Design & JPA Entity

#### Logic Walkthrough
Your `urls` table needs these columns at minimum:
- `id` — auto-incrementing primary key (BIGINT)
- `long_url` — the original URL (TEXT, since URLs can be long)
- `short_code` — the generated short code (VARCHAR(10), unique, indexed)
- `created_at` — timestamp with time zone
- `hit_count` — number of times the short link was used (BIGINT, default 0)

The `short_code` column needs a **unique constraint** and a **separate index**. The unique constraint ensures no duplicates (enforced at the DB level as a safety net). The index makes lookups fast — every redirect hits `WHERE short_code = ?`, so this is the hottest query in your system.

In JPA, your entity class maps to this table. Use `@Entity`, `@Table(name = "urls")`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)` for the auto-increment PK. Use `@Column(unique = true, nullable = false)` on `shortCode` and `@Column(name = "long_url", nullable = false, columnDefinition = "TEXT")` on the long URL.

For `createdAt`, use `@CreationTimestamp` from Hibernate — it auto-populates the field when the entity is first persisted. You don't have to set it manually.

**Gotcha:** JPA's `GenerationType.IDENTITY` delegates PK generation to the database (using a serial/sequence). This means you can't batch inserts efficiently because each insert needs to round-trip to the DB to get the generated ID. For a URL shortener this is fine — you're not doing bulk inserts. But be aware of it as a limitation.

#### Reading Resource
[Baeldung — Spring Data JPA Entity](https://www.baeldung.com/jpa-entities)

**YouTube Search:** `"JPA entity mapping annotations explained PostgreSQL"`

#### Where You'll See This Again
1. This exact schema pattern — a code/token column with a unique index for fast lookup — appears in every authentication system (session tokens, API keys, password reset tokens).
2. Link-in-bio tools (Linktree, etc.) use this same table structure under the hood with added metadata columns.
3. Your job queue project (Project 3 in your breadth plan) will have a similar indexed `status` column pattern for job lookup — same design principle.

**Why this matters:** A missing index on `short_code` means your redirect endpoint does a full table scan for every request. At 1000 URLs that's fine. At 1 million it's a disaster. Indexing the right columns is the single highest-leverage database optimization.

---

### Subproblem 2.2 — Short Code Generation Strategy

#### Logic Walkthrough
You need to convert a long URL into a short, unique code. There are two main approaches:

**Approach A: Hash-based**  
Take the long URL, hash it (MD5, SHA-256, etc.), take the first 6-8 characters of the hex output. Simple, but you'll get collisions — two different URLs can produce the same prefix. You need a collision check: after generating a code, query the DB; if it exists and maps to a different URL, append a character and retry. This gets messy.

**Approach B: Base62 encoding of auto-increment ID** (recommended for this project)  
The DB generates a unique integer ID for every row. You convert that integer to base62 (characters 0-9, a-z, A-Z = 62 symbols). This is mathematically guaranteed to be unique because the underlying ID is unique. The algorithm:

1. Start with your integer ID (e.g., `125`)
2. While the number is greater than 0: take `number % 62` → look up character at that index in your alphabet → prepend to result → divide number by 62
3. Repeat

For ID `125`: `125 % 62 = 1` → 'b', `125 / 62 = 2`, `2 % 62 = 2` → 'c', result is "cb". That's your short code.

The catch: you need the DB-generated ID before you can generate the code, but you need the code to store the row. Two ways to handle this:
- Save the row first with a placeholder, get the generated ID, compute the code, update the row. Two DB writes.
- Pre-generate codes using a separate ID sequence, not tied to the row PK. Cleaner but more complex.

For this project, the two-write approach is fine. Save with `short_code = null`, flush to get the ID, compute the code, update.

**Gotcha:** Short codes from base62 of sequential IDs are predictable — someone can enumerate `a`, `b`, `c`... and harvest all your URLs. If privacy matters, add a random salt or shuffle your alphabet. For this learning project, note it as a known limitation.

#### Reading Resource
[System Design — URL Shortener (ByteByteGo)](https://bytebytego.com/courses/system-design-interview/design-a-url-shortener)

**YouTube Search:** `"base62 encoding URL shortener short code generation"`

#### Where You'll See This Again
1. Coupon code generators, referral codes, and invite tokens all use this same encode-a-sequential-ID approach.
2. YouTube video IDs are base64-encoded integers — same concept, different alphabet.
3. In distributed systems, Snowflake IDs (used by Twitter/X) extend this idea to generate globally unique IDs across multiple machines without coordination — the natural next step from this pattern.

**Why this matters:** Understanding the tradeoffs between hash-based and ID-based generation is a common system design interview question. You're building the intuition for it here.

---

## Part 3: Core REST API & JSON Handling
**Time:** 3–4 hours  
**Goal:** Build the two core endpoints — shorten a URL and redirect from a short code — with proper request/response design and error handling.

*Now that the entity, repository, and short code logic exist, you can build the service and controller layers on top of them.*

### Subproblem 3.1 — Designing Your DTOs and Validation

#### Logic Walkthrough
A DTO (Data Transfer Object) is a class whose only job is to carry data across a boundary — in this case, between the HTTP layer and your service layer. You do NOT use your JPA entity classes directly as request/response bodies. Here's why: your entity has fields like `id`, `createdAt`, and `hitCount` that you don't want clients setting. Separating the DTO from the entity gives you control.

For the shorten endpoint, you need:
- A **request DTO** with one field: the long URL the client is submitting
- A **response DTO** with the short URL to return

On the request DTO, put `@NotBlank` to reject empty strings and `@URL` (from Hibernate Validator, or write a custom one) to reject non-URL strings. Add `@Valid` on the controller method parameter — this is the trigger that activates validation. Without `@Valid`, your annotations do nothing.

When validation fails, Spring throws a `MethodArgumentNotValidException`. By default this returns a 400 with a verbose internal error body. You'll override this in Subproblem 3.3 with a global handler that returns a clean JSON error.

**Gotcha:** `@URL` from Hibernate Validator is strict — it rejects URLs without a scheme (`google.com` fails, `https://google.com` passes). Decide whether you want that behavior or want to be more permissive with a regex-based custom constraint. For this project, strict is fine.

#### Reading Resource
[Baeldung — Spring Boot Validation](https://www.baeldung.com/spring-boot-bean-validation)

**YouTube Search:** `"Spring Boot DTO validation @Valid example"`

#### Where You'll See This Again
1. Every API at scale uses DTOs — GraphQL resolvers, gRPC generated classes, and REST controllers all have this separation between wire format and internal domain model.
2. When you add the chat app (Project 2), WebSocket message payloads follow the same DTO pattern — you'll define a message DTO for incoming chat messages.
3. In your Federal Reserve internship context, request routing software typically validates the shape of a request before it hits the routing logic — same principle, different layer.

**Why this matters:** Validation at the boundary is the first line of defense. Letting raw, unvalidated input reach your service layer and database is how SQL injection and data corruption happen.

---

### Subproblem 3.2 — Building the Shorten and Redirect Endpoints

#### Logic Walkthrough
You have two endpoints to build:

**POST /api/shorten**  
Takes a long URL, generates a short code, saves it, and returns the full short URL. The logic:
1. Validate the incoming DTO (happens automatically via `@Valid`)
2. Check if the long URL already exists in the DB — if it does, return the existing short code instead of creating a duplicate
3. If it's new, generate a short code (built in Part 2), save to DB, return the short URL

What to return: a response DTO containing the short URL (`http://localhost:8080/{code}`), the original URL, and a creation timestamp. Return HTTP 201 Created (not 200) — this is the semantically correct status for resource creation. Use `ResponseEntity.status(HttpStatus.CREATED).body(responseDto)`.

**GET /{shortCode}**  
This is the redirect endpoint. The logic:
1. Look up the short code in cache first (covered in Part 4), then DB
2. If not found, throw a custom `ShortCodeNotFoundException`
3. If found, optionally increment a hit counter
4. Return an HTTP 302 redirect to the long URL

For the redirect, use `ResponseEntity` with a `Location` header:
```java
return ResponseEntity.status(HttpStatus.FOUND)
    .location(URI.create(longUrl))
    .build();
```

**301 vs 302:** 301 is permanent (browser caches it forever), 302 is temporary (browser re-checks each time). Use 302 for URL shorteners — if you ever delete or change a short code, 301 will break for users who have it cached.

**Gotcha:** The `@PathVariable` on `/{shortCode}` will match ANY path segment including things like `/favicon.ico` that browsers request automatically. Either add a prefix (`/r/{shortCode}`) or handle those gracefully in your exception handler.

#### Reading Resource
[Baeldung — Spring REST Controller](https://www.baeldung.com/spring-controller-vs-restcontroller)

**YouTube Search:** `"Spring Boot REST API redirect 302 ResponseEntity example"`

#### Where You'll See This Again
1. Every URL shortener (bit.ly, tinyurl) uses this exact 302 redirect pattern at massive scale — the redirect endpoint is the hot path.
2. OAuth2 callback flows use the same `Location` header redirect — after authentication, the auth server redirects back to your app via this mechanism.
3. In your reverse proxy project (Part 2, Go), you'll implement redirect logic at the HTTP parser level — understanding it at the Spring abstraction first makes the raw implementation clearer.

**Why this matters:** The redirect endpoint is the one that gets called at scale — every click on a short link hits it. Understanding the HTTP semantics of redirects now will inform the caching strategy in Part 4.

---

### Subproblem 3.3 — Global Exception Handling

#### Logic Walkthrough
Without a global exception handler, Spring returns its default error response — a verbose object with `timestamp`, `status`, `error`, `trace`, and `path`. This leaks internal details and is inconsistent. You want a single place that intercepts all exceptions and maps them to clean JSON error responses.

Create a class annotated with `@RestControllerAdvice`. This makes it a component that Spring applies to all controllers globally. Inside, define methods annotated with `@ExceptionHandler(SomeException.class)` — Spring calls the right method when that exception is thrown anywhere in your controller layer.

You'll handle at least three cases:
1. `MethodArgumentNotValidException` (validation failure) → 400 with a list of which fields failed
2. `ShortCodeNotFoundException` (your custom exception) → 404
3. `Exception` (catch-all) → 500 with a generic message

For the validation case, `MethodArgumentNotValidException` has a `getBindingResult()` method that gives you the list of field errors. Extract the field name and the default message from each one and return them as a list in your error response body.

Define a consistent error response DTO: `{ "status": 400, "message": "Validation failed", "errors": [...] }`. Every error from your API looks the same shape.

**Gotcha:** Exception handler method ordering matters when using inheritance. If you have a handler for `Exception` and one for `ShortCodeNotFoundException`, Spring uses the most specific match. But if you put the `Exception` handler in a different class than the specific ones, the ordering can become unpredictable. Keep them all in one `@RestControllerAdvice` class.

#### Reading Resource
[Baeldung — Exception Handling in Spring Boot REST API](https://www.baeldung.com/exception-handling-for-rest-with-spring)

**YouTube Search:** `"Spring Boot @ControllerAdvice global exception handler tutorial"`

#### Where You'll See This Again
1. Every production REST API has this layer — Netflix's Zuul gateway, AWS API Gateway, and your own services all centralize error handling to avoid inconsistent responses.
2. gRPC has the equivalent in `StatusRuntimeException` — a single error model that all RPC calls return, analogous to your standardized error DTO.
3. When you build the API gateway in Part 2 of your roadmap, you'll implement this same "catch and transform" pattern for upstream errors coming back through the proxy.

**Why this matters:** Consistent error responses are what make APIs usable. If some endpoints return `{ "error": "..." }` and others return `{ "message": "..." }`, clients have to write special-case handling for every endpoint.

---

## Part 4: Caching Hot URLs with Redis
**Time:** 3–4 hours  
**Goal:** Implement a cache-aside pattern so popular short codes don't hit the database on every redirect.

*The REST API works end-to-end at this point. Now you're optimizing the hot path — the redirect endpoint — by adding a cache layer in front of the database.*

### Subproblem 4.1 — Cache-Aside Pattern with Redis

#### Logic Walkthrough
The cache-aside pattern is the most common caching strategy. The application manages the cache directly — it's not automatic. Here's the exact logic for your redirect endpoint:

```
1. Receive request for /{shortCode}
2. Check Redis: GET shortCode
3. If cache HIT: return the long URL from Redis (done — no DB call)
4. If cache MISS:
   a. Query PostgreSQL for the short code
   b. If not found in DB: throw 404
   c. If found: write to Redis (SET shortCode longUrl EX 3600)
   d. Return the long URL
```

The `EX 3600` sets a TTL (time-to-live) of 1 hour. After that, Redis automatically evicts the key. This is important because: (a) it prevents your cache from filling up with URLs nobody clicks anymore, and (b) if you ever update or delete a URL, the cache entry will eventually expire.

With Spring Data Redis, you can do this two ways:
- **Manual with `StringRedisTemplate`:** `redisTemplate.opsForValue().get(shortCode)` and `redisTemplate.opsForValue().set(shortCode, longUrl, Duration.ofHours(1))`. Verbose but explicit.
- **Declarative with `@Cacheable`:** Annotate your service method with `@Cacheable(value = "urls", key = "#shortCode")`. Spring wraps the method call — on a cache hit, it returns the cached value without calling your method at all. Cleaner, but the TTL configuration is less obvious and requires setting up a `RedisCacheConfiguration` bean.

For learning purposes, do the manual approach first so you see exactly what's happening, then optionally refactor to `@Cacheable`.

**Gotcha:** `@Cacheable` only works when the method is called through a Spring proxy — i.e., from another bean, not from within the same class. If you call a `@Cacheable` method from another method in the same service, the cache is bypassed entirely. This is one of the most common Spring gotchas.

#### Reading Resource
[Baeldung — Spring Boot Redis Cache](https://www.baeldung.com/spring-boot-redis-cache)

**YouTube Search:** `"cache aside pattern Redis Spring Boot explained"`

#### Where You'll See This Again
1. CDNs (Cloudflare, CloudFront) use this pattern at the network level — cache the response, serve from edge, go to origin only on miss.
2. Your reverse proxy in Part 2 will implement a caching layer — you'll be writing this same logic in Go at the HTTP response level.
3. Database connection pools use a similar hit/miss/populate pattern — reuse a connection if available, create a new one if not.

**Why this matters:** The redirect endpoint is your hot path — every click hits it. Without caching, every click is a DB roundtrip. A cache hit is typically 1–2ms; a DB query is 5–20ms. At scale, that difference is the difference between a system that handles 10k req/s and one that handles 100k req/s.

---

### Subproblem 4.2 — Cache Invalidation on URL Deletion

#### Logic Walkthrough
Cache invalidation is famously hard. For this project, keep it simple: if you add a DELETE `/api/shorten/{shortCode}` endpoint, you need to evict the cache entry when a URL is deleted. Otherwise, deleted short codes still redirect for up to 1 hour (your TTL).

The logic:
1. Delete from PostgreSQL
2. Delete from Redis: `redisTemplate.delete(shortCode)`
3. Return 204 No Content

If using `@Cacheable`, use `@CacheEvict(value = "urls", key = "#shortCode")` on the delete method — Spring handles the eviction automatically.

Think about the ordering: should you delete from the DB first or the cache first? If you delete from the DB and then the app crashes before evicting the cache, you have a dangling cache entry pointing to a deleted URL — but it expires within the TTL. If you delete from the cache first and crash before the DB delete, the URL is still in the DB and the cache just gets repopulated on next access. The second failure mode is less bad. Delete from cache first, then DB.

**Gotcha:** In a multi-instance deployment (multiple copies of your app running), each instance has its own local memory but they share Redis. This is exactly why you use Redis over in-memory caching (like Caffeine/Guava) for anything that runs on more than one server. Since you're running one instance locally, this doesn't matter yet — but note it.

#### Reading Resource
[AWS — Caching Strategies (Cache-Aside, Write-Through)](https://docs.aws.amazon.com/whitepapers/latest/database-caching-strategies-using-redis/caching-patterns.html)

**YouTube Search:** `"cache invalidation strategies explained"`

#### Where You'll See This Again
1. E-commerce product pages cache inventory counts — when a purchase is made, the cache entry must be invalidated or decremented immediately.
2. In your API gateway project (Part 2 roadmap), cache invalidation is a core design decision — how long do you cache upstream responses?
3. Browser cache headers (`Cache-Control: max-age`) are the client-side version of this — the same TTL logic, applied at the HTTP response level.

**Why this matters:** Most caching bugs in production aren't about the cache miss path — it's stale data on cache hit after an update. Understanding invalidation strategies at this small scale prepares you for the harder version of this problem.

---

## Part 5: SOAP Integration
**Time:** 3–4 hours  
**Goal:** Expose the same shorten/lookup functionality via a SOAP endpoint to understand contract-first API design vs REST's resource-first approach.

*Everything works via REST at this point. This part adds a second transport layer on top of the same service logic — you're not rewriting anything, just exposing it differently.*

### Subproblem 5.1 — Contract-First Design with XSD and WSDL

#### Logic Walkthrough
REST is code-first — you write the controller, Spring figures out the shape. SOAP is contract-first — you define the contract (an XSD schema + a WSDL file) and generate code from it. This inversion matters for legacy enterprise systems where the contract is the source of truth.

Here's the flow:
1. Write an XSD file that defines the shape of your request and response XML elements
2. The WSDL is generated automatically by Spring-WS from your XSD (you don't write it by hand)
3. Spring-WS inspects incoming XML, validates it against the XSD, and routes it to the right endpoint method

For your URL shortener SOAP service, define two operations: `ShortenUrl` (takes a long URL, returns a short URL) and `LookupUrl` (takes a short code, returns the long URL).

Your XSD needs request and response types for each:
```xml
<xs:element name="ShortenUrlRequest">
  <xs:complexType>
    <xs:sequence>
      <xs:element name="longUrl" type="xs:string"/>
    </xs:sequence>
  </xs:complexType>
</xs:element>

<xs:element name="ShortenUrlResponse">
  <xs:complexType>
    <xs:sequence>
      <xs:element name="shortUrl" type="xs:string"/>
    </xs:sequence>
  </xs:complexType>
</xs:element>
```

Place this XSD in `src/main/resources/`. Spring-WS serves it at `/ws/*.wsdl`.

**Gotcha:** Spring-WS maps incoming SOAP requests to your endpoint methods by the name of the root XML element in the request body. The method must be annotated with `@PayloadRoot(namespace = "...", localPart = "ShortenUrlRequest")`. If the namespace in your XSD doesn't match the annotation, you get a silent routing failure with no error.

#### Reading Resource
[Spring Web Services Reference — Getting Started](https://docs.spring.io/spring-ws/docs/current/reference/html/#tutorial)

**YouTube Search:** `"Spring WS SOAP web service tutorial contract first"`

#### Where You'll See This Again
1. Banking and insurance systems still use SOAP extensively — the contract-first model ensures strict backward compatibility across teams with multi-year integration timelines.
2. gRPC (which you'll use in Project 3) is the modern equivalent — you define a `.proto` file (the contract) and generate code from it, same principle.
3. OpenAPI/Swagger specs for REST APIs are moving toward the same contract-first model — define the spec, generate the server stubs.

**Why this matters:** Understanding why contract-first exists (strict versioning, cross-team coordination, generated clients) gives you context for why gRPC won out over SOAP in modern systems — and why SOAP still hasn't died in enterprise environments.

---

## Key Concepts to Pause On

These are worth the extra time to understand deeply — they'll show up in interviews and in the Go projects:

- **Cache-aside vs write-through:** Why you chose cache-aside and what write-through would look like
- **301 vs 302 redirects:** Browser caching behavior and why it matters for short links
- **Base62 encoding:** The math, and why sequential IDs make codes predictable
- **Contract-first vs code-first APIs:** The philosophical difference and why enterprise chose SOAP
- **Index design:** Why `short_code` needs an index and what a query plan looks like without it (run `EXPLAIN ANALYZE` in psql to see the difference)
