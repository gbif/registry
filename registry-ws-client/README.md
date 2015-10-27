# Registry WS Client

This project provides the WS client to the registry web services. It implements the GBIF Registry API.

The registry-ws client can be configured in the following ways:

## Read-only mode

Example:

```java
Properties props = new Properties();
// set this to the web service URL.  It might be localhost:8080 for local development
props.setProperty("registry.ws.url", "http://api.gbif.org/v1/");
Injector webserviceClient = Guice.createInjector(new RegistryWsClientModule(props), new AnonymousAuthModule());
DatasetService ds = injector.getInstance(DatasetService.class);
````

## Read-write mode

This is controlled by the `GbifApplicationAuthModule`.  Example:

```java
Properties props = new Properties();
// set this to the web service URL.  It might be localhost:8080 for local development
props.setProperty("registry.ws.url", "http://api.gbif.org/v1/");
// setup authentication using a GBIF account with correct role and permissions
SingleUserAuthModule auth = new SingleUserAuthModule("username", "password");
Injector webserviceClient = Guice.createInjector(new RegistryWsClientModule(props), auth);
DatasetService ds = injector.getInstance(DatasetService.class);
````
