# GBIF Registry Integration Tests

Integration tests for all registry modules using JUnit 5, Mockito, and
Testcontainers (PostgreSQL + Elasticsearch — Docker required).

## Running

### Unit tests only (no Docker, no external dependencies)
```mvn verify```

### Integration tests (requires Docker)
```mvn verify -Pintegration-tests```

### Against real GBIF infrastructure (GBIF staff only)
Add the `registry-local` profile to `~/.m2/settings.xml` (see `registry-ws/README.md`),
then:
```mvn verify -Pintegration-tests,registry-local```

## Configuration

Test properties are in `src/test/resources/application-test.yml`.

Safe default values for all properties are provided automatically via the
`registry-test-defaults` Maven profile in the root pom. No `settings.xml`
configuration is needed to run tests locally.

## IntelliJ IDEA

Right click `src/test/java` → Run 'All Tests'

Right click a specific test class → Run 'TestClassNameIT'
