# GBIF Registry Web Services

This project provides the web service and JavaScript-based admin console for the registry, which operates against PostgreSQL.

## **Warnings**

* This project makes use of Liquibase to manage schemas, **and will truncate tables** in tests.
* Tests are run against the database in JUnit during the Maven verify phase
* Grizzly is used for tests, and port conflicts can be avoided by using `-D`

## Building

To use this project you would typically do one of the following:

* `mvn -Pregistry-local-it clean verify package`
* `mvn -Pregistry-local jetty:run`

with a Maven profile similar to:

````xml
<!-- A profile that allows for local development -->
<profile>
  <id>registry-local</id>
  <properties>
    <registry.db.host>localhost</registry.db.host>
    <registry.db.name>registry</registry.db.name>
    <registry.db.username>tim</registry.db.username>
    <registry.db.password/>
    <registry.db.poolSize>6</registry.db.poolSize>
    <registry.db.connectionTimeout>1000</registry.db.connectionTimeout>

    <appkeys.file>/Users/tim/dev/appkeys.properties</appkeys.file>
    <api.url>http://localhost/</api.url>

    <drupal.db.host>localhost</drupal.db.host>
    <drupal.db.name>drupal</drupal.db.name>
    <drupal.db.username>root</drupal.db.username>
    <drupal.db.password></drupal.db.password>
    <drupal.db.poolSize>6</drupal.db.poolSize>
    <drupal.db.connectionTimeout>1000</drupal.db.connectionTimeout>

    <registry.postalservice.enabled>false</registry.postalservice.enabled>
    <registry.messaging.hostname>mq.gbif.org</registry.messaging.hostname>
    <registry.messaging.port>5672</registry.messaging.port>
    <registry.messaging.virtualhost>/users/trobertson</registry.messaging.virtualhost>
    <registry.messaging.username>trobertson</registry.messaging.username>
    <registry.messaging.password>password</registry.messaging.password>

    <mail.devemail.enabled>false</mail.devemail.enabled>
    <mail.smtp.host>localhost</mail.smtp.host>
    <mail.cc></mail.cc>
    <mail.from>tim@mailinator.com</mail.from>
  </properties>
</profile>
````
