# GBIF Registry CLI

This module provides a CLI service that listens for updates to DOIs and updates the registry database accordingly.

## Usage

The project is built as a single JAR, and run using a configuration file.  Examples are in the `example-confs` directory.

````shell
java -jar registry-cli.jar doi-updater --log-config logback-doi-updater.xml --conf registry-doi-updater.yaml
java -jar registry-cli.jar directory-update --log-config logback-directory-update.xml --conf directory-update.yaml
````

### doi-updater
Listen to DoiUpdate messages, take DOI updates and send them to DataCite. Updates the status of the DOI in the Registry database.

### directory-update
Used to update the participant/node information of the Registry from the Directory API.