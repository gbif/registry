# GBIF Registry Web Services

This project provides the web service for the registry, which operates against PostgreSQL
and Elasticsearch. Project uses [Spring Boot](https://github.com/spring-projects/spring-boot).

## Building

To build this project

 ```mvn clean package```

Add this to local setting.xml (MacOS: `/Users/youruser/.m2/settings.xml`)

````xml
<!-- A profile that allows for local development -->
        <profile>
            <id>registry-local</id>
            <properties>
                <checklistbank-it-spring.db.url>jdbc:postgresql://builds.gbif.org:5432/clb_spring</checklistbank-it-spring.db.url>
                <checklistbank-it-spring.db.name>checklistbank</checklistbank-it-spring.db.name>
                <checklistbank-it-spring.db.username>checklistbank</checklistbank-it-spring.db.username>
                <checklistbank-it-spring.db.password>%checklistbank_password%</checklistbank-it-spring.db.password>

                <appkeys.file>/Users/youruser/dev/appkeys.properties</appkeys.file>
                <api.url>http://api.gbif.org/v1/</api.url>
                <portal.url>https://www.gbif-dev.org</portal.url>

                <registry.postalservice.enabled>false</registry.postalservice.enabled>
                <registry.messaging.hostname>mq.gbif.org</registry.messaging.hostname>
                <registry.messaging.port>5672</registry.messaging.port>
                <registry.messaging.virtualhost>/dev</registry.messaging.virtualhost>
                <registry.messaging.username>doi</registry.messaging.username>
                <registry.messaging.password>%queue_password%</registry.messaging.password>

                <mail.devemail.enabled>false</mail.devemail.enabled>
                <mail.smtp.host>smtp.gmail.com</mail.smtp.host>
                <mail.smtp.port>587</mail.smtp.port>
                <mail.cc></mail.cc>
                <mail.bcc></mail.bcc>
                <mail.from>youruser@gmail.com</mail.from>
                <mail.username>youremail@gmail.com</mail.username>
                <mail.password>%your_email_password%</mail.password>
                <mail.helpdesk>gbifregistry@mailinator.com</mail.helpdesk>

                <directory.app.key>gbif.portal</directory.app.key>
                <directory.app.secret>%directory_app_secret%</directory.app.secret>

                <datacite.api.base.url>https://api.test.datacite.org/</datacite.api.base.url>
                <datacite.user>GBIF.GBIF</datacite.user>
                <datacite.password>%datacite_password%</datacite.password>

                <pipelines.do.all.threads>1</pipelines.do.all.threads>
            </properties>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>
        <profile>
            <id>registry-local-it</id>
            <properties>
                <registry-it.db.url>jdbc:postgresql://localhost:5432/registry_it</registry-it.db.url>
                <registry-it.db.host>localhost</registry-it.db.host>
                <registry-it.db.name>registry_it</registry-it.db.name>
                <registry-it.db.username>mpodolskiy</registry-it.db.username>
                <registry-it.db.password/>

                <registry-it-spring.db.url>jdbc:postgresql://localhost:5432/registry_it</registry-it-spring.db.url>
                <registry-it-spring.db.host>localhost</registry-it-spring.db.host>
                <registry-it-spring.db.name>registry_it</registry-it-spring.db.name>
                <registry-it-spring.db.username>mpodolskiy</registry-it-spring.db.username>
                <registry-it-spring.db.password/>

                <appkeys.testfile>/Users/youruser/dev/appkeys.properties</appkeys.testfile>
            </properties>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>
````

 * Some passwords and secret properties are absent, see gbif-configuration.

 * Make sure database is already created (see [persitence module](../registry-persistence/README.md)).

 * Create appkeys.properties file under the path of property `appkeys.file` and `appkeys.testfile`

 * Gmail example configuartion (can be any).

## Running

To run this project

```java -jar registry-ws/target/registry-ws-1.0-SNAPSHOT-exec.jar```

Execute this command in root directory. Version may be different.

[Parent](../README.md)

