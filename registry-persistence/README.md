# registry-persistence

Update database manually by liquibase-maven-plugin (use values for db.url, db.username, db.password):

```
mvn liquibase:update -Dliquibase.url=<db.url> -Dliquibase.username=<db.username> -Dliquibase.password=<db.password> -Dliquibase.changeLogFile=src/main/resources/liquibase/master.xml -Dliquibase.defaultSchemaName=public
```


