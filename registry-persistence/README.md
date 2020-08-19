# GBIF Registry Persistence

This module includes persistence related functionality: [Liquibase](https://www.liquibase.org/) migration scripts and [MyBatis](https://mybatis.org/mybatis-3/) mapper interfaces\xml.

## Create

Create an empty PostgreSQL database.
Name of the created database should be identical in these places:

 * maven properties `registry.db.name` and `registry.db.url` (see [ws module configuration](../registry-ws/README.md))

 * java property `registry.datasource.url` in [application.yml](../registry-ws/src/main/resources/application.yml)

 * java property `registry.datasource.url` in [application-test.yml](../registry-integration-tests/src/test/resources/application-test.yml)


## Update
Update database manually by liquibase-maven-plugin (use values for db.url, db.username, db.password):

```
mvn liquibase:update -Dliquibase.url=<db.url> -Dliquibase.username=<db.username> -Dliquibase.password=<db.password> -Dliquibase.changeLogFile=src/main/resources/liquibase/master.xml -Dliquibase.defaultSchemaName=public
```

Run script under registry-persistence directory.

In case of exception:

```
Error setting up or running Liquibase: liquibase.exception.SetupException: liquibase/some-changelog.xml does not exist
```

run the following maven command:

```
mvn process-resources
```

DB on the environments (dev, uat, prod) should be updated by scripts.

[Parent](../README.md)
