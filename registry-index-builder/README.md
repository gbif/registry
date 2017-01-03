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
 - go through all occurrence datasets, for each query the occurrence solr server for all distinct taxon keys & year values and update the taxon key coverage field of the respective documents in solr


### How to run the Oozie workflow
To run the workflow use the script [install-workflow](install-workflow.sh)  ```./install-workflow.sh dev gitOAuthToken``` which requires 2 command line parameters:
  
 - profile/environment name: properties file name as is stored in the GBIF configuration repository at the location [https://github.com/gbif/gbif-configuration/tree/master/registry-index-builder](https://github.com/gbif/gbif-configuration/tree/master/registry-index-builder).
 - Github OAuth token: Github authentication token to access the private repository [https://github.com/gbif/gbif-configuration/](https://github.com/gbif/gbif-configuration/) where the configuration files are stored.

The configuration file used by this workflow requires the following settings:
  
```
# oozie
hdfs.namenode=hdfs://nameservice1
hadoop.jobtracker=c1n2.gbif.org:8032
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
