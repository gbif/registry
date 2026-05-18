Async Executor and Metrics (Prometheus)
=====================================

This document explains the bounded async executor introduced in `AsyncConfig`, the configuration properties you can tune, the metrics exposed (Micrometer) and how to scrape them with Prometheus. It also documents the bean name/qualifier and suggested operational behavior.

Files and bean
--------------
- Bean created in: `org/gbif/registry/ws/config/AsyncConfig` (qualifier: `@Qualifier("boundedTaskExecutor")`).
- Bean type: Spring `ThreadPoolTaskExecutor` (returned as `Executor`).
- Profile: the existing `AsyncConfig` is annotated with `@Profile("!test")` in the project; the bean will not be created when the `test` profile is active.

Configuration properties
------------------------
The executor can be tuned via the following configuration keys (defaults shown):

```yaml
# Tune the bounded task executor
registry:
  async:
    corePoolSize: 10
    maxPoolSize: 50
    queueCapacity: 500
```

If you use Spring Boot, these configuration keys can also be set as environment variables by converting the property path to uppercase with dots replaced by underscores, e.g. `REGISTRY_ASYNC_COREPOOLSIZE`.

Bean usage
----------
- Qualifier name: `boundedTaskExecutor` (inject with `@Qualifier("boundedTaskExecutor")`).
- The codebase uses the bean as optional injection in controllers/resources; if it is not present the code falls back to `CompletableFuture.supplyAsync(...)` without an explicit executor.
- The bean should be present in production profiles to ensure controlled concurrency for blocking workloads.

Metrics exposed (Micrometer -> Prometheus)
-----------------------------------------
The executor is instrumented with Micrometer gauges and the async exception handler is instrumented with counters. If you already export to Prometheus, these metrics will appear in your scrape target.

Exposed metric names and labels (as registered by the code):

1) Executor gauges (registered on the underlying `ThreadPoolExecutor`):
- `registry.async.executor.active` - current active thread count (gauge)
- `registry.async.executor.poolSize` - current pool size (gauge)
- `registry.async.executor.queueSize` - current queue size (gauge)

These are simple numeric gauges and are useful to detect saturation: growing `queueSize` + `active` near `maxPoolSize` indicates executor is overloaded.

2) Async exception counters:
- `registry.async.exceptions{type="completion|execution|illegal_argument", cause="<ExceptionClass>", status="<http-status>"}`

Examples of metric labels:
- `registry.async.exceptions{type="completion",cause="NullPointerException",status="503"}`
- `registry.async.exceptions{type="completion",cause="IllegalArgumentException",status="400"}`

Notes about labels:
- `type` indicates which top-level exception handler saw the exception (e.g. CompletionException).
- `cause` is the simple class name of the nested cause when available.
- `status` is the HTTP status code chosen by the `AsyncExceptionHandler` mapping.

Prometheus scraping
-------------------
If your application exposes Micrometer metrics via a Prometheus endpoint (e.g. Spring Boot Actuator ` /actuator/prometheus`), add a scrape job in Prometheus configuration pointing to that endpoint. Example `prometheus.yml` snippet:

```yaml
scrape_configs:
  - job_name: 'registry-ws'
    static_configs:
      - targets: ['registry-host:port']
    metrics_path: /actuator/prometheus
```

Grafana / Prometheus queries examples
------------------------------------
- Current active threads: `registry_async_executor_active`
- Executor queue size: `registry_async_executor_queueSize`
- Exceptions per minute (all types):
```
rate(registry_async_exceptions[1m])
```
- Exceptions filtered by status 503 in last 5m:
```
sum(rate(registry_async_exceptions{status="503"}[5m])) by (cause)
```

(Depending on your Prometheus metrics naming normalization you may need to replace dots with underscores; the Prometheus exporter used by Micrometer transforms metric names. The examples above assume the direct mapping; adapt queries by inspecting the `/actuator/prometheus` payload.)

Operational guidance and tuning
--------------------------------
- Start with conservative settings (e.g. core 10, max 50, queue 500) and increase based on CPU and I/O characteristics. If tasks are blocking (I/O, DB calls, external APIs) you may need more threads but watch memory.
- Monitor `registry.async.executor.queueSize` and `registry.async.executor.active`. If queue grows steadily, either increase capacity or reduce incoming load.
- Consider adding a `RejectedExecutionHandler` policy if you want immediate backpressure (e.g. `CallerRunsPolicy` or a custom handler that increments metrics and returns HTTP 429/503). The current implementation does not install a custom RejectedExecutionHandler.
- The `boundedTaskExecutor` bean is injected optionally; ensure the bean is available on the profile you run in production.

AsyncExceptionHandler and HTTP mapping
-------------------------------------
- There is a `ControllerAdvice` (`AsyncExceptionHandler`) that maps `CompletionException` / `ExecutionException` and nested causes into HTTP responses. It also increments `registry.async.exceptions` counters with labels for `type`, `cause` and `status`.
- The default mappings implemented are:
  - `IllegalArgumentException` -> HTTP 400 (Bad Request)
  - other nested causes -> HTTP 503 (Service Unavailable)

If you prefer more specific mappings (timeouts -> 504, connection refused -> 502), update the `AsyncExceptionHandler` to inspect nested causes (e.g. `SocketTimeoutException`, `ConnectException`) and map accordingly.

Next steps / suggestions
------------------------
- If you only use Prometheus, ensure Micrometer's Prometheus registry is on the classpath and the `/actuator/prometheus` endpoint or an equivalent is exposed for scraping.
- Optionally implement a `RejectedExecutionHandler` that records a metric and returns an immediate error to callers when the executor is saturated.
- If you want, I can perform a repo-wide replacement of all remaining `CompletableFuture.supplyAsync(...)` calls to pass the `boundedTaskExecutor` explicitly and compile.

Location
--------
This README was added at:
`registry-ws/ASYNC_EXECUTOR_README.md`


If you want any edits to this README (more Prometheus/Grafana examples, JSON error body samples, or a sample Alertmanager rule for saturated executor), tell me which items to add and I'll update the file.

