[![Build Status](https://builds.gbif.org/job/registry-spring-boot/badge/icon?plastic)](https://builds.gbif.org/job/registry-spring-boot/)
[![Quality Gate Status](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-parent&metric=alert_status)](https://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-parent)
[![Coverage](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-parent&metric=coverage)](http://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-parent)

# registry-spring-boot

# Code style

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
