[![Build Status](https://builds.gbif.org/job/registry-spring-boot/badge/icon?plastic)](https://builds.gbif.org/job/registry-spring-boot/)
[![Quality Gate Status](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-spring-boot-parent&metric=alert_status)](https://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-spring-boot-parent)
[![Coverage](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-spring-boot-parent&metric=coverage)](http://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-spring-boot-parent)

# GBIF Registry

The GBIF Registry is a core component of the architecture responsible for providing the authoritative source of information on GBIF participants (Nodes), institutions (e.g. data publishers), datasets, networks their interrelationships and the means to identify and access them.

As a distributed network, the registry serves a central coordination mechanism, used for example to allow publishers to declare their existence and for data integrating components to discover how to access published datasets and interoperate with the publisher.

## Code style

The registry uses google code style and spotless.
To configure an automatic pre-commit hook to check code add a file 'pre-commit' to directory .git/hooks.
File's content should be:

```
#!/bin/sh

echo '[git hook] executing spotless check before commit'

# stash any unstaged changes
git stash -q --keep-index

# run the check with the maven
mvn spotless:check

# store the last exit code in a variable
RESULT=$?

# unstash the unstashed changes
git stash pop -q

# return the 'mvn spotless:check' exit code
exit $RESULT
```

## Modules
 Project modules:
 - [**registry-cli**](registry-spring-boot-cli/README.md)
 - [**registry-directory**](registry-spring-boot-directory/README.md)
 - [**registry-directory-client**](registry-spring-boot-directory-client/README.md)
 - [**registry-doi**](registry-spring-boot-doi/README.md)
 - [**registry-domain**](registry-spring-boot-domain/README.md)
 - [**registry-events**](registry-spring-boot-events/README.md)
 - [**registry-identity**](registry-spring-boot-identity/README.md)
 - [**registry-integration-tests**](registry-spring-boot-integration-tests/README.md)
 - [**registry-mail**](registry-spring-boot-mail/README.md)
 - [**registry-messaging**](registry-spring-boot-messaging/README.md)
 - [**registry-metadata**](registry-spring-boot-metadata/README.md)
 - [**registry-oaipmh**](registry-spring-boot-oaipmh/README.md)
 - [**registry-occurrence-client**](registry-spring-boot-occurrence-client/README.md)
 - [**registry-persistence**](registry-spring-boot-persistence/README.md)
 - [**registry-pipelines**](registry-spring-boot-pipelines/README.md)
 - [**registry-search**](registry-spring-boot-search/README.md)
 - [**registry-security**](registry-spring-boot-security/README.md)
 - [**registry-service**](registry-spring-boot-service/README.md)
 - [**registry-surety**](registry-spring-boot-surety/README.md)
 - [**registry-ws**](registry-spring-boot-ws/README.md)
