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
