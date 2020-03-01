# GBIF Registry Integration Tests

This module includes integration tests for all the registry modules.
Internally using [JUnit](https://junit.org/junit4/),
[Mockito](https://site.mockito.org/) and
[Cucumber](https://docs.cucumber.io/).

 ## Run tests by maven

 ### Run all tests

 ```mvn clean verify```

 ### Run specific test

 Each major feature test has its own tag (e.g. `@Dataset`, `@IPT`). See [feature files](src/test/resources/features).

 One specific tag can be started

 ```mvn clean verify -Dcucumber.options="--tags @tagname"```

 or several ones

 ```mvn clean verify -Dcucumber.options="--tags @tagname1 @tagname2"```

 Tag annotations can be added to scenarios if needed for temporal test purposes and can be removed afterwards.
 Tag annotations are repeateable.

 ## Run by Intellij IDEA

 Right click on java directory -> Run 'All Tests'

 Right click on specific test class -> Run 'ThisIT'

 ## Cucumber integration test structure

 Most of the tests have the following structure:

 * Main test class named '*IT' which is entry point. Should be annotated with `@RunWith(Cucumber.class)` and `@CucumberOptions`.
 Cucumber options must contain two mandatory parameters: `features` (list of feature files' paths, e.g. `classpath:features/installation.feature`)
 and `glue` (package where test logic is placed, called `*TestSteps`).

 * Feature file with all the scenarios for this integration test. See syntax details at [Cucumber docs](https://docs.cucumber.io/).
 All feature files are in `src/test/resources/features`.

 * Test steps class.

 * (Optional) SQL preparation scripts. Scripts are in `src/test/resources/scripts`

 ## Configuration

 Test properties can be configured in file [application-test.yml](src/test/resources/application-test.yml).

[Parent](../README.md)
