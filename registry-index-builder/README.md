# Registry Index Builder

This project covers the Java classes and Oozie workflows needed to backfill the dataset index of the registry solr index.
The solr schema itself is defined in the [registry-ws](../registry-ws/src/main/resources/solr/) module.
To avoid too many large dependencies such as Oozie within the webservice we separated the code into its own module.

There is one Oozie workflow in this project which can be configured to:
 - create a new dataset index from scratch, wiping anything existing and backfill it with the latest datasets from the registry.
 - update the dataset index with metrics from ChecklistBank for datasets of type CHECKLIST
 - update the dataset index with metrics from the Occurrence store for datasets of type OCCURRENCE or EVENT

## Building the Solr Index
The standard way to build a new solr index is to run the Oozie [workflow](src/main/resources/dataset-backfill/workflow.xml) from a cluster gateway machine. The installation script and following Oozie actions will:

 - create a new collection from scratch or delete any previously existing one
 - loop over all datasets using mybatis and load them into solr
 - go through all checklist datasets in ChecklistBank and update the taxon key coverage field of the respective documents in solr
 - go through all occurrence datasets, for each query the occurrence solr server for all distinct taxon keys, countries & years and update the respective dataset solr index field


### How to run the Oozie workflow
To run the workflow use the script [install-workflow](install-workflow.sh)  ```./install-workflow.sh dev gitOAuthToken``` which requires 2 command line parameters:

 - profile/environment name: properties file name as is stored in the GBIF configuration repository at the location [https://github.com/gbif/gbif-configuration/tree/master/registry-index-builder](https://github.com/gbif/gbif-configuration/tree/master/registry-index-builder).
 - Github OAuth token: Github authentication token to access the private repository [https://github.com/gbif/gbif-configuration/](https://github.com/gbif/gbif-configuration/) where the configuration files are stored.

The configuration file used by this workflow requires the following settings:

```
# oozie
namenode=hdfs://nameservice1
jobtracker=c1n2.gbif.org:8032
oozie.url=http://c1n2.gbif.org:11000/oozie
oozie.use.system.libpath=true
oozie.wf.application.path=hdfs://nameservice1/registry-index-builder-dev/
oozie.libpath=hdfs://nameservice1/registry-index-builder-dev/lib/
oozie.launcher.mapreduce.task.classpath.user.precedence=true
user.name=yarn
environment=dev

# solr installation details
solr.home=/opt/cloudera/parcels/SOLR5/
solr.url=http://c2n1.gbif.org:8983/solr
solr.opts=numShards=1&replicationFactor=1&maxShardsPerNode=1
# dataset index
solr.dataset.type=CLOUD
solr.dataset.home=c1n1.gbif.org:2181,c1n2.gbif.org:2181,c1n6.gbif.org:2181/solr5dev
solr.dataset.collection=dev_dataset
# occurrence index
solr.occurrence.type=CLOUD
solr.occurrence.home=c1n1.gbif.org:2181,c1n2.gbif.org:2181,c1n6.gbif.org:2181/solr5dev
solr.occurrence.collection=dev_occurrence

#Thread pool configuration
solr.indexing.threads=2

# registry db
registry.db.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
registry.db.dataSource.serverName=pg1.gbif-dev.org
registry.db.dataSource.databaseName=dev_registry
registry.db.dataSource.user=registry
registry.db.dataSource.password=*****
registry.db.maximumPoolSize=4

# directory
directory.ws.url=http://api.gbif-dev.org/v1/directory/
directory.app.key=gbif.portal
directory.app.secret=*****

# checklistbank db to read taxon coverage from
checklistbank.db.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
checklistbank.db.dataSource.serverName=pg1.gbif-dev.org
checklistbank.db.dataSource.databaseName=dev_clb
checklistbank.db.dataSource.user=clb
checklistbank.db.dataSource.password=*****
checklistbank.db.maximumPoolSize=2
```

### Managing the SOLR index.

After building the index, we can add a replica:
```
curl -i 'http://c5n1.gbif.org:8983/solr/admin/collections?action=ADDREPLICA&collection=dataset_2018_09_21&shard=shard1&node=c5n6.gbif.org:8983_solr'
```

and update the alias:
```
curl -i 'http://c5n1.gbif.org:8983/solr/admin/collections?action=CREATEALIAS&name=dataset&collections=dataset_2018_09_21'
```

and delete the old index:
```
curl 'http://c5n1.gbif.org:8983/solr/admin/collections?action=DELETE&name=dataset_2018_02_09'
```

## Installing the Oozie coordinator job
The same workflow can be executed as an Oozie coordinator job running once daily using the same configs as for the workflow above.
The existing solr collection will not be dropped but all 3 main steps will be executed:
  - loop over all datasets using mybatis and load them into solr
  - go through all checklist datasets in ChecklistBank and update the taxon key coverage field of the respective documents in solr
  - go through all occurrence datasets, for each query the occurrence solr server for all distinct taxon keys & year values and update the taxon key coverage field of the respective documents in solr

To install the coordinator job, removing any previous coordinator job,
use the script [install-coordinator.sh](install-coordinator.sh)  ```./install-coordinator.sh dev gitOAuthToken``` which again requires the 2 command line parameters described above.
