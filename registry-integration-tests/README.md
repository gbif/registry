# GBIF Registry Integration Tests

This module includes integration tests for all the registry modules.
Internally using [JUnit](https://junit.org/junit5/),
[Mockito](https://site.mockito.org/) and
testcontainers.

 ## Run tests by maven

 ```mvn clean verify```

 ## Run by Intellij IDEA

 Right click on java directory -> Run 'All Tests'

 Right click on specific test class -> Run 'ThisIT'

 ## Configuration

 Test properties can be configured in file [application-test.yml](src/test/resources/application-test.yml).

[Parent](../README.md)
