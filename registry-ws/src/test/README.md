# Tests

 - Test Guice bindings: `org.gbif.registry.ws.guice.RegistryWsServletListenerTest`
 - Guice configuration for Integration testings: `org.gbif.registry.guice.RegistryTestModules`

### Tests using a embedded GrizzlyWebServer
 - Server extending TestRule: `org.gbif.registry.grizzly.RegistryServer`
 - ServletListener: `org.gbif.registry.guice.TestRegistryWsServletListener`

-> Output of the Grizzly server is available in the `target/surefire-reports`.

