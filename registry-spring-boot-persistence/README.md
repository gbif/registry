# GBIF Registry Persistence

This module includes persistence related stuff: [Liquibase](https://www.liquibase.org/) migration scripts and [MyBatis](https://mybatis.org/mybatis-3/) mapper interfaces\xml.

## Create

Create an empty PostgreSQL database.
Name of the created database should be identical in these places:

 * maven properties `registry.db.name` and `registry.db.url` (see [ws module configuration](../registry-spring-boot-ws/README.md))

 * java property `registry.datasource.url` in [application.yml](../registry-spring-boot-ws/src/main/resources/application.yml)

 * java property `registry.datasource.url` in [application-test.yml](../registry-spring-boot-integration-tests/src/test/resources/application-test.yml)


## Update
Update database manually by liquibase-maven-plugin (use values for db.url, db.username, db.password):

```
mvn liquibase:update -Dliquibase.url=<db.url> -Dliquibase.username=<db.username> -Dliquibase.password=<db.password> -Dliquibase.changeLogFile=src/main/resources/liquibase/master.xml -Dliquibase.defaultSchemaName=public
```

[Parent](../README.md)
