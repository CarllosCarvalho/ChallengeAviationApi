# Airport Service

A production-grade microservice that accepts HTTP requests to fetch airport details
by ICAO code, backed by the [Aviation API v2](https://api-v2.aviationapi.com).

---

## Tech stack

| Concern | Choice | Rationale |
|---|---|---|
| Runtime | Java 21 + Virtual Threads | Loom virtual threads eliminate thread-pool sizing concerns for I/O-bound workloads |
| Framework | Spring Boot 3.3 | Baseline for the team; first-class integration with all libraries used |
| HTTP Client | OpenFeign + OkHttp | Declarative, readable; OkHttp gives a proper connection pool & keep-alive |
| Resilience | Resilience4j (CB + Retry + RateLimiter) | Composable, annotation-driven; does not require a service mesh |
| Metrics | Micrometer + Prometheus | Standard Spring Boot observability stack |
| Docs | SpringDoc / Swagger UI | Auto-generated from annotations; zero maintenance overhead |
| Tests | JUnit 5 + Mockito + WireMock | WireMock allows realistic upstream contract testing |

---

## Running the service

```bash
# Build & run
./mvnw spring-boot:run

# Or as a JAR
./mvnw package -DskipTests
java -jar target/airport-service-1.0.0.jar
```

The service starts on **port 8080**.

---

## API

### `GET /api/v1/airports/{icao}`

Returns airport details and navigation charts for the requested ICAO code.

**Path parameter:** `icao` — 4-letter ICAO identifier (case-insensitive). Example: `KJFK`, `SBGR`, `EGLL`.

**Success response (200):**

```json
{
  "icao": "KJFK",
  "iata": "JFK",
  "name": "John F Kennedy Intl",
  "city": "New York",
  "state": "NY",
  "country": "USA",
  "military": false,
  "charts": {
    "airportDiagram": [
      { "name": "AIRPORT DIAGRAM", "pdfUrl": "https://...", "changed": false }
    ],
    "departure": [
      {
        "name": "DEEZZ SIX (RNAV)",
        "pdfUrl": "https://...",
        "changed": true,
        "changePdfUrl": "https://.../_cmp.pdf"
      }
    ],
    "approach": [ ... ],
    "arrival":  [ ... ],
    "general":  [ ... ]
  }
}
```

**Error responses:**

| Status | Condition |
|---|---|
| 400 | ICAO is not exactly 4 alphanumeric characters |
| 404 | Airport not found in upstream data |
| 503 | Upstream API unreachable or circuit breaker is OPEN |
| 502 | Unexpected upstream gateway error |

All errors follow a consistent envelope:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Airport not found for ICAO: ZZZZ",
  "path": "/api/v1/airports/ZZZZ",
  "timestamp": "2024-06-01T12:00:00Z"
}
```

**Swagger UI:** http://localhost:8080/swagger-ui.html

---

## Resilience design

```
Caller → [RateLimiter] → [Retry] → [CircuitBreaker] → Feign → Aviation API
```

The three Resilience4j mechanisms wrap every outbound call in this order:

### RateLimiter
- **Limit:** 20 requests / second to the upstream API
- **Behaviour on breach:** immediate `RequestNotPermitted` exception → 503 to caller
- **Purpose:** self-protection against accidentally DDoS-ing a free third-party API

### Retry
- **Max attempts:** 3 (initial + 2 retries)
- **Back-off:** exponential starting at 500 ms → 1 s → 2 s
- **Retried on:** `IOException`, `TimeoutException`, HTTP 503, HTTP 504
- **Not retried:** 4xx client errors, `InvalidIcaoException` — these are deterministic failures; retrying would not help and wastes quota

### Circuit Breaker
- **Window:** last 10 calls (COUNT_BASED)
- **Opens at:** 50% failure rate (5/10 calls failing)
- **Wait in OPEN:** 30 seconds, then transitions to HALF_OPEN
- **HALF_OPEN probes:** 3 calls; if they succeed → CLOSED
- **Fallback method** returns `null`; the service layer detects this and raises `UpstreamUnavailableException` → 503

**Why null fallback instead of cached data?**
Returning stale chart data (which includes PDF URLs) could be actively harmful in aviation contexts — a chart revised in the current AIRAC cycle must be the current version. A clear 503 is safer than silently serving outdated navigation information. A cache with a short TTL (e.g. 60 s) and explicit `stale` flag could be added as a future improvement.

---

## Observability

### Health & metrics endpoints (Actuator)

| Endpoint | Description |
|---|---|
| `GET /actuator/health` | Liveness + readiness + circuit breaker state |
| `GET /actuator/metrics` | All Micrometer metrics |
| `GET /actuator/prometheus` | Prometheus scrape endpoint |
| `GET /actuator/circuitbreakers` | Circuit breaker state details |

### Custom metrics

- **`airport.lookup`** — Timer recording upstream call latency, tagged with `icao`

### Structured logging

- MDC key `icao` is injected for the duration of each request → every log line emitted during the request automatically includes the airport code
- Log pattern: `%d [%thread] %-5level [%X{icao}] %logger - %msg`

---

## Project structure

```
src/main/java/com/aviation/airport/
│
├── AirportServiceApplication.java      # Entry point
│
├── controller/
│   └── AirportController.java          # HTTP routing, Swagger docs
│
├── service/
│   └── AirportService.java             # Business logic, validation, MDC, metrics
│
├── feign/
│   ├── AviationApiProvider.java        # Port (interface) — service depends on this
│   ├── AviationApiClient.java          # Feign declarative client
│   ├── AviationApiClientAdapter.java   # Adapter: bridges port → Feign + Resilience4j
│   └── dto/
│       └── AviationApiResponse.java    # Raw upstream JSON model
│
├── mapper/
│   └── AirportMapper.java             # Raw → public DTO transformation
│
├── dto/
│   ├── AirportDetailsResponse.java    # Public API response model
│   └── ErrorResponse.java            # Uniform error envelope
│
├── exception/
│   ├── InvalidIcaoException.java
│   ├── AirportNotFoundException.java
│   ├── UpstreamUnavailableException.java
│   └── GlobalExceptionHandler.java    # @RestControllerAdvice
│
└── config/
    └── FeignConfig.java               # OkHttp pool, timeouts, logger level
```

---

## Design decisions & trade-offs

### Provider abstraction (`AviationApiProvider` interface)
The service layer is coded to a `AviationApiProvider` interface, not the Feign client directly. This means swapping to a different upstream (OurAirports, AeroDataBox, etc.) requires only a new `@Component` implementing the interface — zero changes to the service or controller. It also makes the service trivially unit-testable with a `mock(AviationApiProvider.class)`.

### Two retry layers (Feign vs Resilience4j)
Feign has a built-in `Retryer`. It is deliberately set to `NEVER_RETRY` in `FeignConfig`. All retry logic lives in Resilience4j so behaviour is centralised, configurable without redeployment (Actuator `/refresh`), and observable via metrics.

### Virtual Threads
`spring.threads.virtual.enabled=true` routes all incoming request handling onto Java 21 virtual threads. This is especially beneficial here because the service is almost entirely I/O-bound (one blocking Feign call per request). Traditional thread-pool exhaustion under load is eliminated without any async/reactive complexity.


## API Documentation (Swagger / OpenAPI)

### Setup

Add the SpringDoc dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

Add the following block to `application.yml`:

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha
    tags-sorter: alpha
  show-actuator: false
```

Create the configuration bean `OpenApiConfig.java`:

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI airportServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Airport Service API")
                        .description("Microservice for fetching airport details by ICAO code")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Aviation Team")
                                .email("aviation@example.com"))
                        .license(new License()
                                .name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local")
                ));
    }
}
```

### Accessing the docs

With the application running (`./mvnw spring-boot:run`):

| URL | Description |
|---|---|
| `http://localhost:8080/swagger-ui.html` | Interactive Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI spec (JSON) |
| `http://localhost:8080/v3/api-docs.yaml` | OpenAPI spec (YAML) |
