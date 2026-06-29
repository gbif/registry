# GBIF Registry Web Services

This project provides the web service for the registry, which operates against PostgreSQL
and Elasticsearch. Project uses [Spring Boot](https://github.com/spring-projects/spring-boot).

## Building

```mvn clean package```

## Running

```java -jar registry-ws/target/registry-ws-1.0-SNAPSHOT-exec.jar```

Execute this command in root directory. Version may be different.

## Local development against GBIF infrastructure (GBIF staff only)

> **Not required** for building the project or running tests locally — those work
> out of the box with safe defaults. Only needed to run against real GBIF dev
> infrastructure or external services.

Add the following profiles to your local `~/.m2/settings.xml`.

> **Important:** Do not add `<activeByDefault>true</activeByDefault>` to these profiles.
> Profiles must be activated explicitly to avoid silently pointing your build at
> real GBIF infrastructure.

```xml
<profile>
  <id>registry-local</id>
  <properties>
    <checklistbank-it-spring.db.url>jdbc:postgresql://builds.gbif.org:5432/clb_spring</checklistbank-it-spring.db.url>
    <checklistbank-it-spring.db.name>checklistbank</checklistbank-it-spring.db.name>
    <checklistbank-it-spring.db.username>checklistbank</checklistbank-it-spring.db.username>
    <checklistbank-it-spring.db.password>FILL_ME_IN</checklistbank-it-spring.db.password>

    <appkeys.file>/path/to/your/appkeys.properties</appkeys.file>
    <api.url>http://api.gbif.org/v1/</api.url>
    <portal.url>https://www.gbif-dev.org</portal.url>

    <registry.postalservice.enabled>false</registry.postalservice.enabled>
    <registry.messaging.hostname>mq.gbif.org</registry.messaging.hostname>
    <registry.messaging.port>5672</registry.messaging.port>
    <registry.messaging.virtualhost>/dev</registry.messaging.virtualhost>
    <registry.messaging.username>doi</registry.messaging.username>
    <registry.messaging.password>FILL_ME_IN</registry.messaging.password>

    <mail.devEmailForIdentity.enabled>true</mail.devEmailForIdentity.enabled>
    <mail.devEmailForOrganizationsEndorsement.enabled>true</mail.devEmailForOrganizationsEndorsement.enabled>
    <mail.devEmailForCollections.enabled>true</mail.devEmailForCollections.enabled>
    <mail.devEmailForPipelines.enabled>true</mail.devEmailForPipelines.enabled>
    <mail.smtp.host>smtp.gmail.com</mail.smtp.host>
    <mail.smtp.port>587</mail.smtp.port>
    <mail.cc></mail.cc>
    <mail.bcc></mail.bcc>
    <mail.from>youruser@gmail.com</mail.from>
    <mail.username>youruser@gmail.com</mail.username>
    <mail.password>FILL_ME_IN</mail.password>
    <mail.helpdesk>gbifregistry@mailinator.com</mail.helpdesk>

    <directory.ws.url>http://api.gbif-dev.org/v1/directory/</directory.ws.url>
    <directory.app.key>gbif.portal</directory.app.key>
    <directory.app.secret>FILL_ME_IN</directory.app.secret>

    <datacite.api.base.url>https://api.test.datacite.org/</datacite.api.base.url>
    <datacite.user>GBIF.GBIF</datacite.user>
    <datacite.password>FILL_ME_IN</datacite.password>

    <pipelines.do.all.threads>1</pipelines.do.all.threads>
  </properties>
</profile>

<profile>
  <id>registry-local-it</id>
  <properties>
    <registry-it.db.url>jdbc:postgresql://localhost:5432/registry_it</registry-it.db.url>
    <registry-it.db.host>localhost</registry-it.db.host>
    <registry-it.db.name>registry_it</registry-it.db.name>
    <registry-it.db.username>FILL_ME_IN</registry-it.db.username>
    <registry-it.db.password/>

    <registry-it-spring.db.url>jdbc:postgresql://localhost:5432/registry_it</registry-it-spring.db.url>
    <registry-it-spring.db.host>localhost</registry-it-spring.db.host>
    <registry-it-spring.db.name>registry_it</registry-it-spring.db.name>
    <registry-it-spring.db.username>FILL_ME_IN</registry-it-spring.db.username>
    <registry-it-spring.db.password/>

    <appkeys.testfile>/path/to/your/appkeys.properties</appkeys.testfile>
  </properties>
</profile>
```

Secrets and passwords are not listed here — see gbif-configuration for the actual values.

Make sure the database is already created before running (see [persistence module](../registry-persistence/README.md)).

Create an `appkeys.properties` file at the path referenced by `appkeys.file` and `appkeys.testfile`.
The file is a simple key=secret properties file, one app key per line.

```properties
gbif.portal=FILL_ME_IN
gbif.registry-ws-client=FILL_ME_IN
gbif.app.it=FILL_ME_IN
```

actual secrets are available in gbif-configuration

### Running against GBIF dev infrastructure

```mvn verify -Pintegration-tests,registry-local```

### Running integration tests with a local database

```mvn verify -Pintegration-tests,registry-local-it```

[Parent](../README.md)
