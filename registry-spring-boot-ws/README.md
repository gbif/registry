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

                <registry.db.url>jdbc:postgresql://localhost:5432/registry</registry.db.url>
                <registry.db.host>localhost</registry.db.host>
                <registry.db.name>registry</registry.db.name>
                <registry.db.username>youruser</registry.db.username>
                <registry.db.password>%db_password%</registry.db.password>
                <registry.db.poolSize>6</registry.db.poolSize>
                <registry.db.connectionTimeout>1000</registry.db.connectionTimeout>

                <appkeys.file>/Users/youruser/dev/appkeys.properties</appkeys.file>
                <api.url>http://api.gbif.org/v1/</api.url>

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

                <directory.ws.url></directory.ws.url>
                <directory.app.key></directory.app.key>
                <directory.app.secret></directory.app.secret>

                <datacite.api.base.url>https://api.test.datacite.org/</datacite.api.base.url>
                <datacite.user>GBIF.GBIF</datacite.user>
                <datacite.password>%datacite_password%</datacite.password>

                <pipelines.do.all.threads>1</pipelines.do.all.threads>
            </properties>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>
````

 * Some secret properties are absent, see gbif-configuration.

 * Make sure database is already created (see [persitence module](../registry-spring-boot-persistence/README.md)).

 * Create appkeys.properties file under the path of property `appkeys.file`

 * Gmail example configuartion (can be any).

## Running

To run this project

```java -jar registry-spring-boot-ws/target/registry-spring-boot-ws-1.0-SNAPSHOT-exec.jar```

Execute this command in root directory. Version may be different.

[Parent](../README.md)

