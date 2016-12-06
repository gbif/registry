# Registry Solr

This project covers the Oozie workflows needed to backfill the dataset index of the registry solr index.
The plain index is defined in the registry-ws module. To avoid too many large dependencies such as ChecklistBank, Oozie and Spark 
we separated the code into its own module.

There is one Oozie workflow in this project which can be configured to:
 - create a new dataset index from scratch, wiping anything existing and backfill it with the latest datasets from the registry.
 - update the dataset index with metrics from ChecklistBank for datasets of type CHECKLIST
 - update the dataset index with metrics from the Occurrence store for datasets of type OCCURRENCE or EVENT

## Building the Solr Index

The recommended way to build a complete Solr (cloud) index is via Oozie from scratch is:


### Oozie [workflow](src/main/resources/oozie/workflow.xml)  
This workflow has the capability of building Solr indexes for both cloud  and non-cloud servers. 
The non-cloud index is produced in a single shard index stored in HDFS, it will be copied into the Solr server. 
In general terms the workflow steps are: 
    * Export name usages to Avro files into HDFS using a multithreaded java application running on one node
    * Run a map reduce index builder using the Avro files as input
    * If it's a Solr cloud index, then install it into the target cluster using the provided Zookeeper chroot.
    * If it's a single shard index, keep the produced index in the output directory.
    * bind the configured solr collection name as an alias to the newly created collection (which uses a date)

#### How to install/run the Oozie workflow
 The simplest way of doing it is using the script [install-workflow](install-workflow.sh), it requires 2 command line parameters:
  * profile/environment name: properties file name as is stored in the GBIF configuration repository at the location [https://github.com/gbif/gbif-configuration/tree/master/checklistbank-index-builder](https://github.com/gbif/gbif-configuration/tree/master/checklistbank-index-builder).
  * Github OAuth token: Github authentication token to access the private repository [https://github.com/gbif/gbif-configuration/](https://github.com/gbif/gbif-configuration/) where the configuration files are stored.
  The configuration file used by this workflow requires the following settings:
  
```
hadoop.jobtracker=prodmaster3-vh.gbif.org:8032
hdfs.namenode=hdfs://ha-nn
oozie.url=http://oozie.gbif.org:11000/oozie
solr.home=/opt/solr-5.3.1/
solr.zk=prodmaster1-vh.gbif.org:2181,prodmaster2-vh.gbif.org:2181,prodmaster3-vh.gbif.org:2181/prodclbsolr
solr.http.url=http://prodsolr01-vh.gbif.org:8983/solr
hdfs.out.dir=hdfs://ha-nn/checklistbank-index-builder-prod/output/
solr.collection=prod_checklistbank
solr.collection.opts=numShards=3&amp;replicationFactor=2&amp;maxShardsPerNode=2
hadoop.client.opts=-Xmx4096m
mapred.opts=-Dmapreduce.reduce.shuffle.input.buffer.percent=0.2 -Dmapreduce.reduce.shuffle.parallelcopies=5 -Dmapreduce.map.memory.mb=4096 -Dmapreduce.map.java.opts=-Xmx3584m -Dmapreduce.reduce.memory.mb=8192 -Dmapreduce.reduce.java.opts=-Xmx7680m
oozie.use.system.libpath=true
oozie.wf.application.path=hdfs://ha-nn/checklistbank-index-builder-prod/
oozie.libpath=hdfs://ha-nn/checklistbank-index-builder-prod/lib/
oozie.launcher.mapreduce.task.classpath.user.precedence=true
#using user yarn because the staging dir is created under yarn user's folder
user.name=yarn
environment=prod


#Thread pool configuration
checklistbank.indexer.threads=20
checklistbank.indexer.batchSize=5000
checklistbank.indexer.writers=1
checklistbank.indexer.logInterval=60
checklistbank.indexer.nameNode=hdfs://ha-nn/
checklistbank.indexer.targetHdfsDir=/checklistbank/solr/prod/name_usage/

# leave blank to use embedded solr server
checklistbank.indexer.solr.server=
checklistbank.indexer.solr.delete=false
checklistbank.indexer.solr.server.type=EMBEDDED

# mybatis
checklistbank.db.dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
checklistbank.db.dataSource.serverName=pg1.gbif.org
checklistbank.db.dataSource.databaseName=checklistbank
checklistbank.db.dataSource.user=clb
checklistbank.db.dataSource.password=...
checklistbank.db.maximumPoolSize=28
checklistbank.db.connectionTimeout=10000
checklistbank.db.leakDetectionThreshold=0

# registry
registry.ws.url=http://api.gbif.org/
```

#### Examples

To install/copy the Oozie workflow in the dev environment and create a cloud shard index run:
  
  ```
  ./install-workflow.sh dev gitOAuthToken
  ```
    
The workflow can also be executed using custom properties file using the Oozie client, this once the workflow was already installed on HDFS:
    
  ```
    oozie job --oozie http://oozieserver:11000/oozie/ -config custom.properties -run
  ```
