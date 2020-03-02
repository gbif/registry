# GBIF Registry Directory

This module includes [gbif-directory](https://github.com/gbif/directory) related functionality.

**IMPORTANT:** This module requires refactoring:

* `directory-api` which includes `gbif-api` with Jackson 1.

* `directory-ws-client` which is old WS Rest Client based on Jersey and Guice.

These features hould be replaced once the new gbif-directory is implemented.

[Some functionality](../registry-spring-boot-ws/src/main/java/org/gbif/directory) is in `registry-ws` module now. It must be moved to this module afterwards.

## Configuration

 * `directory.enabled` disable directory if false and use stubs instead of real classes

 * `directory.app.key` application key for application authentication (e.g. gbif.portal)

 * `directory.app.secret` secret key associated with application key

 * `directory.ws.url` directory URL (e.g. http://api.gbif-dev.org/v1/directory/)

[Parent](../README.md)
