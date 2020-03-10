# GBIF Registry Directory Client

This module contains [directory](https://github.com/gbif/directory) related WS clients.
It uses [OpenFeign client](https://github.com/OpenFeign/feign).

Consider moving this module out of the project.

# Configuration

In order to use clients from this module several properties should be provided:

 * `directory.ws.url` directory API URL (e.g. http://api.gbif-dev.org/v1/directory/)

 * `directory.app.key` application key (e.g. gbif.portal)

 * `directory.app.secret` application secret key

[Parent](../README.md)
