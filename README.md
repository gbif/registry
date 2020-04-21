[![Build Status](https://builds.gbif.org/job/registry/badge/icon?plastic)](https://builds.gbif.org/job/registry/)
[![Quality Gate Status](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-parent&metric=alert_status)](https://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-parent)
[![Coverage](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-parent&metric=coverage)](http://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-parent)

# GBIF Registry

The GBIF Registry is a core component of the architecture responsible for providing the authoritative source of information on GBIF participants (Nodes), institutions (e.g. data publishers), datasets, networks their interrelationships and the means to identify and access them.

As a distributed network, the registry serves a central coordination mechanism, used for example to allow publishers to declare their existence and for data integrating components to discover how to access published datasets and interoperate with the publisher.

## Code style

The registry uses spotless-maven-plugin and google code style.
Please check [motherpom](https://github.com/gbif/motherpom) project for some important configuration.

### Project files

Code style related files. **Please make sure you properly configure required IDE settings**:

- [gbif.importorder](./gbif.importorder) overrides default google package import order (for spotless-maven-plugin).
- [gbif-lecense-header](./gbif-license-header) provides default license header (for spotless-maven-plugin).
- [google-style.xml](./google-style.xml) java google code style, should be imported to IDE as a default one: Preferences --> Editor --> Code Style --> Import scheme (gear next to 'Scheme' dropdown) --> Intellij IDEA code style XML.
- [.editorconfig](./.editorconfig) formatting properties which overrides some inconvenient google ones (e.g. static imports at the beginning of imports list). Make sure editorconfig file is enabled in IDE: Preferences --> Editor --> Code Style --> General --> Enable EditorConfig support.


### Git hooks
To configure automatic git hook to check the code, add the file [pre-commit](pre-commit) to the directory .git/hooks:

```
cp pre-commit .git/hooks/
```

Also, make sure the file has the right permissions. In Unix and Mac it needs the execution permission:

```
chmod +x .git/hooks/pre-commit
```

### Manual use of Spotless

Check the project follows code style conventions:

```
mvn spotless:check
```

Fix code style violations:

```
mvn spotless:apply
```

Sometimes it may not help (e.g. it does not fix two empty lines but consider them as a violation).
In this case run spotless check command above and fix them manually afterwards.

For more information see [documentation](https://github.com/diffplug/spotless/tree/master/plugin-maven).


## Modules
 Project modules:
 - [**registry-cli**](registry-cli/README.md)
 - [**registry-directory**](registry-directory/README.md)
 - [**registry-directory-client**](registry-directory-client/README.md)
 - [**registry-doi**](registry-doi/README.md)
 - [**registry-domain**](registry-domain/README.md)
 - [**registry-events**](registry-events/README.md)
 - [**registry-identity**](registry-identity/README.md)
 - [**registry-integration-tests**](registry-integration-tests/README.md)
 - [**registry-mail**](registry-mail/README.md)
 - [**registry-messaging**](registry-messaging/README.md)
 - [**registry-metadata**](registry-metadata/README.md)
 - [**registry-metasync**](registry-metasync/README.md)
 - [**registry-oaipmh**](registry-oaipmh/README.md)
 - [**registry-occurrence-client**](registry-occurrence-client/README.md)
 - [**registry-persistence**](registry-persistence/README.md)
 - [**registry-pipelines**](registry-pipelines/README.md)
 - [**registry-search**](registry-search/README.md)
 - [**registry-security**](registry-security/README.md)
 - [**registry-service**](registry-service/README.md)
 - [**registry-surety**](registry-surety/README.md)
 - [**registry-ws**](registry-ws/README.md)
 - [**registry-ws-client**](registry-ws-client/README.md)
