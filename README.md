[![Build Status](https://builds.gbif.org/job/registry/badge/icon?plastic)](https://builds.gbif.org/job/registry/)
[![Quality Gate Status](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-motherpom&metric=alert_status)](https://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-motherpom) 
[![Coverage](https://sonar.gbif.org/api/project_badges/measure?project=org.gbif.registry%3Aregistry-motherpom&metric=coverage)](https://sonar.gbif.org/dashboard?id=org.gbif.registry%3Aregistry-motherpom)


# GBIF Registry

The GBIF Registry is a core component of the architecture responsible for providing the authoritative source of information on GBIF participants (Nodes), institutions (e.g. data publishers), datasets, networks their interrelationships and the means to identify and access them.

As a distributed network, the registry serves a central coordination mechanism, used for example to allow publishers to declare their existence and for data integrating components to discover how to access published datasets and interoperate with the publisher.

## To build the project

1. Create an empty PostgreSQL database "registry_it".  You may need to run

    `CREATE EXTENSION unaccent;`
    `CREATE EXTENSION hstore;`

  (installed extensions can be listed with `\dx` from `psql`.)

2. This database will automatically be populated by Liquibase when the integration tests are run.


3. Set up a solr collection hosted either in a simple [http solr server](http://lucene.apache.org/solr/quickstart.html) or in a Solr cloud. Follow the [registry-index-builder](registry-index-builder/README.md) to create and populate such a collection. See the [maven POM](pom.xml) for the minimum solr version required by the current registry code.

4. Create a Maven profile similar to:

````xml
  <profile>
    <id>registry-local-it</id>
    <properties>
      <registry-it.db.host>localhost</registry-it.db.host>
      <registry-it.db.name>registry_it</registry-it.db.name>
      <registry-it.db.username>registry</registry-it.db.username>
      <registry-it.db.password/>
      <appkeys.testfile>/home/mblissett/Workspace/appkeys-it.properties</appkeys.testfile>
        
      <datacite.api.base.url>https://api.test.datacite.org/</datacite.api.base.url>
      <datacite.user>GBIF.GBIF</datacite.user>
      <datacite.password/>
   </properties>
  </profile>
````
datacite.password can be found in configs.

5. Run `mvn clean install`.
