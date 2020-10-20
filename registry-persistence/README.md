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

```commandline
mvn liquibase:update -Dliquibase.url=<db.url> -Dliquibase.username=<db.username> -Dliquibase.password=<db.password>
```

Run script under registry-persistence directory.

In case of exception:

```
Error setting up or running Liquibase: liquibase.exception.SetupException: liquibase/some-changelog.xml does not exist
```

run the following maven command:

```commandline
mvn process-resources
```

**DB on the environments (dev, uat, prod) are updated automatically on application startup.**

## Rollback

Each changeset should contain rollback section:

```xml
<changeSet id="testRollback" author="programmer">
    <createTable tableName="user">
        <column name="id" type="int"/>
        <column name="username" type="varchar(36)"/>
        <column name="age" type="integer"/>
    </createTable>
    <rollback>
        <dropTable tableName="user"/>
    </rollback>
</changeSet>
```

in order to be able to rollback changes automatically in case of exception.

Rollback may be also performed manually. There are several ways to do that.

By count:

```commandline
mvn liquibase:rollback -Dliquibase.rollbackCount=1 -Dliquibase.url=<db.url> -Dliquibase.username=<db.username> -Dliquibase.password=<db.password>
```

`rollbackCount` is a parameter which defines how many changeset will be rolled back.


or by date:

```commandline
mvn liquibase:rollback "-Dliquibase.rollbackDate=Oct 18, 2020" -Dliquibase.url=<db.url> -Dliquibase.username=<db.username> -Dliquibase.password=<db.password>
```

`rollbackDate` is a parameter which defines date; any changeset executed after that day will be rolled back (including the date).

[Parent](../README.md)
