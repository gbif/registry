# GBIF Registry CLI

This module provides a CLI service that listens for updates to DOIs and updates the registry database accordingly.

## Usage

The project is built as a single JAR, and run using a configuration file.  Examples are in the `example-confs` directory.


### doi-updater
Listen to DoiUpdate messages, take DOI updates and send them to DataCite. Updates the status of the DOI in the Registry database.


```shell
java -jar registry-cli.jar doi-updater --log-config logback-doi-updater.xml --conf registry-doi-updater.yaml
```

### directory-update
Used to update the participant/node information of the Registry from the Directory API.

```shell
java -jar registry-cli.jar directory-update --log-config logback-directory-update.xml --conf directory-update.yaml
```

### doi-synchronizer
CLI to display and possibly fix DOI status between GBIF database and Datacite. This CLI is an administrative tool designed to be used
manually.

```shell
java -jar registry-cli.jar doi-synchronizer --log-config logback-doi-synchronizer.xml --conf doi-synchronizer.yaml --doi 10.15468/dl.4d4nny
```

Options:

 * `--doi`: specify a single DOI
 * `--doi-list <DOI file>`: specify a list of DOIs stored in a file, one DOI per line.
 * `--fix-doi`: try to rebuild the DataCite metadata document and to resend it to the `doi-updater` cli. Used with `--doi`
 of `--doi-list`.
 * `--list-failed-doi`: list all DOIs from the database (dataset and download) with the status FAILED
 * `--doi <doi> --export`: export the DataCite metadata document from the database into the file.
 * `--skip-dia`: skip the diagnostic, used with `--doi-list <DOI file> --fix-doi` to not print the diagnostic of each DOIs.

### dataset-updater
Temporary tool to force the update of a dataset in the database by re-interpreting its metadata document (EML).

```shell
java -jar registry-cli.jar dataset-updater --log-config logback-util.xml --conf dataset-updater.yaml --dataset-key e95d0010-b3f1-11de-82f8-b8a03c50a862
```
