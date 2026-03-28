## Part 6: Testing with Testcontainers
**Time:** 4–6 hours  
**Goal:** Build a test suite that verifies your URL shortener against real PostgreSQL and Redis instances running in Docker containers — not mocks, not in-memory substitutes.

*The entire application works end-to-end at this point — URL creation, caching, auth, session management, and ownership scoping. Now you're proving it works correctly and building a safety net that lets you refactor without fear.*

---

### Subproblem 6.1 — Test Infrastructure & Testcontainers Setup

#### Logic Walkthrough
Before you write a single test, you need your test environment to spin up real Postgres and Redis instances. This is what Testcontainers does — it uses Docker to launch containers before your tests run and tears them down after.

Add these dependencies to your `pom.xml`:
- `spring-boot-starter-test` — you likely already have this; it bundles JUnit 5, Mockito, AssertJ, and MockMvc
- `org.testcontainers:postgresql` — the Postgres Testcontainers module
- `org.testcontainers:junit-jupiter` — JUnit 5 integration for Testcontainers
- `com.redis:testcontainers-redis` — the Redis Testcontainers module (or use the generic container with a Redis image)

The key concept: Testcontainers starts a real Docker container, exposes it on a random port, and gives you the connection details at runtime. Your tests need to pick up those dynamic connection details instead of the hardcoded values in `application.properties`.

The cleanest way to do this is with a shared base test configuration. Create an abstract class (e.g., `BaseIntegrationTest`) that:
1. Declares the Postgres and Redis containers as `static` fields (so they're shared across all test classes — one container startup per suite, not per class)
2. Uses `@DynamicPropertySource` to inject the container's connection details into Spring's environment, overriding whatever is in `application.properties`

```
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
}
```

This is the same `StringRedisTemplate` and `DataSource` your application already uses — the only thing that changes is *where* they point. Your application code doesn't know or care that it's talking to a container instead of your Docker Compose services.

Set `spring.jpa.hibernate.ddl-auto=create-drop` in your test configuration so Hibernate creates a fresh schema for every test run and drops it at the end. In development you use `update`; in tests you want a clean slate every time.

**Gotcha:** Testcontainers requires Docker to be running on the machine executing tests. If Docker isn't available (some CI environments, some corporate laptops), the tests will fail at container startup with a connection error, not a clear "Docker not found" message. Verify Docker is running before debugging Testcontainers issues.

**Gotcha:** Making the containers `static` means they're shared across test classes, which is what you want for speed — starting Postgres takes a few seconds, and you don't want to pay that cost per class. But it also means test classes share the same database. You'll need a strategy for data isolation between tests, which is covered in the next subproblem.

#### Reading Resource
[Testcontainers — Getting Started with Spring Boot](https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/)

**YouTube Search:** `"Testcontainers Spring Boot PostgreSQL integration test setup"`

#### Where You'll See This Again
1. This is the standard testing setup at companies using Spring Boot — Testcontainers has become the default for integration tests in the Java ecosystem.
2. The same pattern applies when testing against any external dependency: Kafka, Elasticsearch, MongoDB. Testcontainers has modules for all of them.
3. CI pipelines (GitHub Actions, Jenkins) run these same container-backed tests — the Docker-in-Docker or sidecar approach varies, but the Testcontainers code stays identical.

**Why this matters:** The whole point of integration tests is to catch problems that unit tests can't — dialect differences, constraint violations, connection handling. If your tests run against a different database engine than production, they're not actually testing what you think they're testing.

---

### Subproblem 6.2 — Unit Testing Isolated Logic

#### Logic Walkthrough
Start with the code that has zero dependencies on Spring, the database, or Redis. Your Base62 encoder is the perfect first target — it's a pure function that takes an integer and returns a string.

For the Base62 encoder, test:
- Known conversions: `1 → "b"`, `62 → "ba"`, `125 → "cb"` (verify against your own implementation's alphabet ordering)
- Edge behavior: what does your encoder do with `0`? With `Long.MAX_VALUE`? These aren't necessarily error cases — they're boundary cases that your implementation should handle consistently
- Determinism: the same input always produces the same output

These tests use plain JUnit 5 — no Spring context, no Testcontainers, no annotations beyond `@Test`. They run in milliseconds. This is the fastest feedback loop you have.

Next, unit test your service layer with mocked dependencies. Your `UrlService` depends on the URL repository and `StringRedisTemplate`. In a unit test, you don't want real database calls — you want to test the *logic* of the service method in isolation. This is where Mockito comes in.

Use `@ExtendWith(MockitoExtension.class)` on the test class, `@Mock` on the repository and Redis template, and `@InjectMocks` on the service. Then use `when(...).thenReturn(...)` to control what the mocked dependencies return, and `verify(...)` to assert that the service called them correctly.

For example, testing the redirect flow:
1. Mock Redis to return `null` (cache miss)
2. Mock the repository to return a `Url` entity with a known long URL
3. Call the service method
4. Assert the return value is the expected long URL
5. Verify that Redis `set` was called (the cache-aside write-on-miss)

Then test the cache hit path:
1. Mock Redis to return the long URL directly
2. Call the service method
3. Assert the return value is correct
4. Verify the repository was *never called* — this is the whole point of caching

**Gotcha:** Mocking `StringRedisTemplate` can be verbose because of its `opsForValue()` chaining. You need to mock `opsForValue()` to return a mock `ValueOperations`, then mock the `get`/`set` on that. It's tedious but straightforward — and it's a good exercise in understanding how the template's API is structured.

**Gotcha:** `@InjectMocks` uses constructor injection if available, then setter injection, then field injection. If your service uses constructor injection (which it should), make sure the `@Mock` fields match the constructor parameter types exactly. Mismatches result in `null` dependencies and confusing NPEs.

#### Reading Resource
[Baeldung — Mockito Tutorial](https://www.baeldung.com/mockito-series)

**YouTube Search:** `"JUnit 5 Mockito service layer unit test Spring Boot"`

#### Where You'll See This Again
1. Every service layer in every Spring Boot application gets tested this way — mock the repositories, test the business logic, verify the interactions.
2. The mock-verify pattern is language-agnostic. Go's `gomock`, Python's `unittest.mock`, and JavaScript's `jest.fn()` all work the same way — define expected behavior, run the code, check the calls.
3. In your microservices project, service-to-service calls get mocked the same way — you mock the HTTP client that calls the downstream service and test your logic in isolation.

**Why this matters:** Unit tests with mocks run in milliseconds and catch logic bugs instantly. They're your first line of defense. But they can't catch integration issues — a mock always behaves exactly how you tell it to. That's why you need both unit and integration tests.

---

### Subproblem 6.3 — Repository Integration Tests

#### Logic Walkthrough
Now test your JPA repositories against real Postgres. These tests verify that your entity mappings, constraints, queries, and indexes actually work against the real database engine.

Annotate your test class with `@DataJpaTest`. This loads only the JPA slice of Spring — entities, repositories, and the datasource — without starting the full web server. It's faster than `@SpringBootTest` because it skips controllers, services, security, and Redis. But here's the problem: `@DataJpaTest` auto-configures an embedded database (H2) by default, which is exactly what you're avoiding. You need to tell it to use your Testcontainers Postgres instead.

Add `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` — this tells Spring "don't replace my datasource, I've configured it myself." Combined with the `@DynamicPropertySource` from your base test class, the repository tests now run against real Postgres.

What to test:
- **The unique constraint on `short_code`:** Save two entities with the same short code. Assert that the second save throws a `DataIntegrityViolationException`. This verifies your constraint is actually enforced at the DB level — not just declared in your JPA annotations.
- **The unique constraint on `username`:** Same pattern — two users with the same username should fail.
- **Custom queries:** If you wrote `findByShortCodeAndIsShortCodeActiveTrue(String shortCode)`, test that it returns the entity when `isShortCodeActive` is `true` and returns empty when it's `false`. This is testing your JPQL/method-name query derivation against real Postgres — H2 might parse the query differently.
- **The `@Modifying` hit counter:** Call your atomic increment query, flush, and re-read the entity. Assert the count went up by 1. This verifies that your `@Modifying` + `@Transactional` combination actually commits the update.
- **The `@ManyToOne` relationship:** Save a user, save a URL with that user as owner, then query `findByUserId(userId)`. Assert the URL comes back with the user association intact.

Each test method should set up its own data and not depend on data from another test. Use `@Transactional` on the test class (which `@DataJpaTest` applies by default) — each test runs in a transaction that rolls back at the end, so the database is clean for the next test. This is your data isolation strategy.

**Gotcha:** `@Transactional` rollback in tests can mask bugs. If your production code relies on a flush happening at transaction commit (e.g., to trigger a constraint violation), the test might pass because the flush never happens during rollback. Use `TestEntityManager.flush()` or `repository.saveAndFlush()` explicitly when testing constraint violations.

**Gotcha:** `@DataJpaTest` doesn't load your `SecurityFilterChain`, `PasswordEncoder`, or any `@Service` beans. If your repository tests need to save a `User` entity with a hashed password, you'll need to hash it manually in the test setup (e.g., `new BCryptPasswordEncoder().encode("password")`), not inject the bean.

#### Reading Resource
[Baeldung — Spring Boot @DataJpaTest](https://www.baeldung.com/spring-boot-testing#integration-testing-with-datajpatest)

**YouTube Search:** `"Spring Boot @DataJpaTest integration test PostgreSQL Testcontainers"`

#### Where You'll See This Again
1. Repository tests are the safety net for database migrations. When you add a column, change a constraint, or modify a query, these tests tell you immediately if something broke.
2. In systems with complex query logic (reporting, analytics, search), repository-level tests are often more valuable than service-level tests because the query correctness *is* the business logic.
3. When you switch from method-name query derivation to native SQL (for performance), these tests verify the native query returns identical results to the derived one.

**Why this matters:** Your repository layer is the boundary between your application and your data. A bug here — a wrong query, a missing constraint, a broken mapping — corrupts data silently. Repository tests are cheap to write and catch expensive bugs.

---

### Subproblem 6.4 — Controller Tests with MockMvc

#### Logic Walkthrough
Controller tests verify your HTTP layer — status codes, response shapes, validation errors, headers, and content types. They don't test business logic (that's the service test's job) and they don't test database behavior (that's the repository test's job). They test the *contract* between your API and its clients.

Use `@SpringBootTest` with `@AutoConfigureMockMvc`. This starts the full Spring context (including your security filter chain) and gives you a `MockMvc` instance that simulates HTTP requests without starting a real HTTP server.

There's a design choice here: you *could* use `@WebMvcTest(YourController.class)` which loads only the web layer and requires you to `@MockBean` every service dependency. This is faster but means you're mocking the service — you're testing that the controller calls the service correctly, not that the full request actually works. With `@SpringBootTest`, you get the real service, real repository, real Redis — a true integration test from HTTP to database. Since you have Testcontainers providing real Postgres and Redis, lean into the full integration approach.

What to test:

**URL shortening endpoint:**
- Happy path: POST a valid URL, assert 201 status, assert the response body contains a short URL
- Validation: POST with a blank URL, assert 400 status, assert the error response body lists the validation error
- Validation: POST with an invalid URL (not a valid URL format), assert 400 status
- Duplicate: POST the same URL twice as the same user, assert you get the same short code back

**Redirect endpoint:**
- Happy path: GET a valid short code, assert 302 status, assert the `Location` header contains the long URL
- Not found: GET a nonexistent short code, assert 404 status
- Soft-deleted: GET a short code that's been soft-deleted, assert 404 status

**Auth endpoints:**
- Registration: POST valid credentials, assert 201
- Registration with duplicate username: assert 409
- Login: POST valid credentials, assert 200, assert the response includes a `Set-Cookie` header with your session cookie
- Login with wrong password: assert 401
- Logout: POST with a valid session cookie, assert 200, verify the session is gone from Redis

**Protected endpoint access:**
- POST to the shorten endpoint without a session cookie, assert 401 (or 403, depending on your security config)
- POST to the shorten endpoint with a valid session cookie, assert 201

For the auth tests, you'll need a helper method that registers a user, logs in, and extracts the session cookie from the response. Then you pass that cookie into subsequent requests via `MockMvc`'s `.cookie()` method. This is where you're testing the full auth flow end-to-end — from cookie to Redis session lookup to security context to controller.

```
MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(loginJson))
    .andExpect(status().isOk())
    .andReturn();

Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION_ID");

mockMvc.perform(post("/api/v1/short-url")
        .cookie(sessionCookie)
        .contentType(MediaType.APPLICATION_JSON)
        .content(shortenJson))
    .andExpect(status().isCreated());
```

**Gotcha:** `MockMvc` does not execute actual HTTP calls — it goes through Spring's `DispatcherServlet` directly. This means things like CORS headers and servlet filters work, but anything handled by the embedded Tomcat *before* the servlet (like certain SSL configurations) won't be tested. For this project, that distinction doesn't matter. But know it exists.

**Gotcha:** When testing validation error responses, use `jsonPath` assertions to verify the shape: `.andExpect(jsonPath("$.errors").isArray())`. Don't just check the status code — verify the response body matches the contract your global exception handler defines.

#### Reading Resource
[Baeldung — Testing in Spring Boot](https://www.baeldung.com/spring-boot-testing)

**YouTube Search:** `"Spring Boot MockMvc integration test tutorial controller"`

#### Where You'll See This Again
1. MockMvc tests are the standard way to test Spring REST APIs in enterprise codebases. You'll see them in every Spring Boot project you contribute to.
2. The pattern of testing "authenticated vs unauthenticated access" at the controller level is how every team verifies their security configuration isn't accidentally permissive.
3. Contract testing tools like Spring Cloud Contract and Pact build on this same MockMvc foundation — they generate these tests from API contracts.

**Why this matters:** Controller tests are your confidence that the API behaves correctly from the client's perspective. A passing controller test means: the right status code, the right response shape, the right headers, and the right security enforcement. If you refactor the service layer and the controller tests still pass, you know the API contract is intact.

---

### Subproblem 6.5 — Testing Cache Behavior

#### Logic Walkthrough
Your caching layer is one of the most bug-prone parts of the system — stale data, missed invalidations, and cache/DB inconsistencies are the kinds of bugs that unit tests with mocked Redis can't catch. This is where Testcontainers earns its keep.

These tests use `@SpringBootTest` with real Postgres and real Redis (from Testcontainers). You'll interact with `StringRedisTemplate` directly in your test to inspect cache state — the same template your application uses, pointed at the Testcontainers Redis instance.

What to test:

**Cache population on redirect (cache-aside write-on-miss):**
1. Create a short URL (via the service or directly in the DB)
2. Verify Redis does NOT have the key yet (use `redisTemplate.hasKey(shortCode)`)
3. Call the redirect endpoint (or service method)
4. Verify Redis now HAS the key with the correct long URL value
5. Verify the TTL is set (use `redisTemplate.getExpire(shortCode)` — it should be positive and within your expected TTL range)

**Cache hit skips DB:**
This is harder to verify in an integration test because you can't easily assert "the database was not queried." Two approaches:
- Use a `@SpyBean` on the repository. A spy wraps the real bean — it delegates to the real implementation by default, but you can verify whether methods were called. After populating the cache, call redirect again and `verify(urlRepository, never()).findByShortCode(shortCode)`.
- Or: check it indirectly by soft-deleting the URL in the DB (but not evicting the cache), then calling redirect. If the redirect still works, the response came from cache, not the DB.

**Cache invalidation on soft delete:**
1. Create a URL and trigger a redirect to populate the cache
2. Verify the cache entry exists
3. Call the delete endpoint
4. Verify the cache entry is gone (`redisTemplate.hasKey(shortCode)` returns `false`)
5. Verify a subsequent redirect returns 404

**Cache TTL expiry:**
You generally don't want to wait for real TTL expiry in tests (your TTL is an hour). Two options:
- Set a short TTL (e.g., 2 seconds) in your test configuration via `@DynamicPropertySource`, create and cache an entry, sleep briefly past the TTL, then verify the key is gone. This is slow and slightly fragile but tests real expiry.
- Trust that Redis TTL works (it does) and just verify the TTL is *set* correctly rather than waiting for it to fire. This is the pragmatic choice.

**Gotcha:** If you're using `@Transactional` on your test class, be aware that cache writes happen during the transaction but the transaction rolls back at test end. This means the cache has data but the database doesn't — the next test could see stale cache state. For cache integration tests, you may want to skip `@Transactional` on the test class and instead clean up manually (flush Redis between tests with `redisTemplate.getConnectionFactory().getConnection().flushDb()` or use `@DirtiesContext`).

**Gotcha:** `@SpyBean` replaces the bean in the Spring context with a Mockito spy. Unlike `@MockBean` (which replaces with a mock), a spy delegates to the real implementation. This is powerful but has a cost: `@SpyBean` and `@MockBean` both cause the Spring context to be reloaded if the spy/mock combination differs between test classes. Group your spy-based tests in one class to avoid unnecessary context reloads.

#### Reading Resource
[Baeldung — Spring Boot Testing with @SpyBean](https://www.baeldung.com/spring-spy-vs-mock)

**YouTube Search:** `"Spring Boot Redis cache integration test Testcontainers"`

#### Where You'll See This Again
1. Cache correctness tests matter in every system that caches — e-commerce inventory, session stores, API rate limiters. The pattern of "verify cache state directly via the cache client" is universal.
2. The `@SpyBean` technique for verifying "this layer was bypassed" applies whenever you have an optimization layer (cache, circuit breaker, connection pool) that should short-circuit under certain conditions.
3. In distributed systems, cache consistency tests become even more critical — when multiple services write to the same cache, invalidation bugs are the #1 source of stale data incidents.

**Why this matters:** Caching bugs are silent. The system doesn't crash — it just returns wrong data. A user deletes a URL but it still redirects for an hour. A password is changed but the old session still works. These tests are the only way to catch those scenarios before your users do.

---

## Test Organization & Conventions

Keep your test structure mirrored to your main source tree:
```
src/test/java/com/yourpackage/
├── BaseIntegrationTest.java          ← shared Testcontainers setup
├── encoder/
│   └── Base62EncoderTest.java        ← pure unit test, no Spring
├── repository/
│   ├── UrlRepositoryTest.java        ← @DataJpaTest + Testcontainers
│   └── UserRepositoryTest.java
├── service/
│   └── UrlServiceUnitTest.java       ← Mockito, no Spring context
├── controller/
│   ├── UrlControllerTest.java        ← @SpringBootTest + MockMvc
│   └── AuthControllerTest.java
└── cache/
    └── CacheIntegrationTest.java     ← @SpringBootTest + real Redis
```

Name your test methods to describe the scenario and expected outcome, not the method being tested. `redirectShouldReturn404WhenShortCodeNotFound()` tells you more than `testRedirect()`. When a test fails in CI six months from now, the name should tell you what broke without opening the file.

---

## Key Concepts to Pause On

These are worth the extra time to understand deeply — they'll show up in interviews and in future projects:

- **Test pyramid:** Unit tests (fast, many) at the base, integration tests (slower, fewer) in the middle, end-to-end tests (slowest, fewest) at the top. You're building all three layers here.
- **`@DataJpaTest` vs `@SpringBootTest`:** Which Spring context slices load in each, and why the narrower slice is faster but less realistic. Know when to use which.
- **`@MockBean` vs `@SpyBean`:** A mock replaces the real bean entirely (you define all behavior). A spy wraps the real bean (real behavior by default, but you can verify and override). Spies are for "did this get called?" assertions without losing real behavior.
- **`@Transactional` rollback in tests:** Why it provides data isolation, and the edge cases where it hides bugs (constraint violations that need a flush, cache writes that survive rollback).
- **Testcontainers lifecycle:** Static containers are shared across the test suite (fast). Instance containers are created per test class (isolated but slow). The `@DynamicPropertySource` bridge is how the container's random port reaches Spring's configuration.
- **Testing behavior, not implementation:** A good test asserts *what* the system does (returns 302, sets a cookie, populates the cache), not *how* it does it (calls method X, then method Y). Implementation-coupled tests break every time you refactor, even when the behavior is unchanged.
