package org.gbif.registry.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.collections.CollectionResource;
import org.gbif.registry.ws.resources.collections.InstitutionResource;

import java.util.UUID;
import javax.ws.rs.core.Response;

import com.google.inject.Injector;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Tests the {@link org.gbif.registry.ws.resources.collections.IdentifierResolverResource}. */
public class IdentifierResolverIT {

  private static final String IDENTIFIER1 = "http://grbio.org/cool/g9da-xpan";
  private static final String IDENTIFIER2 = "urn:lsid:biocol.org:col:35158";

  @ClassRule
  public static final LiquibaseInitializer liquibaseRule =
      new LiquibaseInitializer(LiquibaseModules.database());

  @ClassRule public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @ClassRule
  public static final DatabaseInitializer databaseRule =
      new DatabaseInitializer(LiquibaseModules.database());

  private static Client client;

  private static UUID collectionKey;
  private static UUID institutionKey;

  @BeforeClass
  public static void setup() {
    // jersey client
    ClientConfig clientConfig = new DefaultClientConfig();
    client = Client.create(clientConfig);

    // get mappers to create some identifiers
    Injector inj = RegistryTestModules.webservice();
    CollectionService collectionService = inj.getInstance(CollectionResource.class);
    InstitutionService institutionService = inj.getInstance(InstitutionResource.class);

    // create collection
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("col1");
    collectionKey = collectionService.create(collection);

    // add identifier to collection
    collectionService.addIdentifier(
        collectionKey, new Identifier(IdentifierType.GRBIO_URI, IDENTIFIER1));

    // there could be duplicates since we don't check it
    collectionService.addIdentifier(
        collectionKey, new Identifier(IdentifierType.GRBIO_URI, IDENTIFIER1));

    // create institution
    Institution institution = new Institution();
    institution.setCode("i1");
    institution.setName("inst1");
    institutionKey = institutionService.create(institution);

    // add identifier to institution
    institutionService.addIdentifier(
        institutionKey, new Identifier(IdentifierType.LSID, IDENTIFIER2));
  }

  @Test
  public void findCollectionByCoolUri() {
    WebResource webResourceResolve =
        client.resource(
            "http://localhost:"
                + RegistryServer.getPort()
                + "/grscicoll/resolve/"
                + IDENTIFIER1.replace("http://:", "dev."));

    ClientResponse response = webResourceResolve.get(ClientResponse.class);

    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    assertTrue(response.getLocation().toString().endsWith("/collection/" + collectionKey));
  }

  @Test
  public void findInstitutionByLsid() {
    WebResource webResourceResolve =
        client.resource(
            "http://localhost:" + RegistryServer.getPort() + "/grscicoll/resolve/" + IDENTIFIER2);

    ClientResponse response = webResourceResolve.get(ClientResponse.class);

    assertEquals(Response.Status.SEE_OTHER.getStatusCode(), response.getStatus());
    assertTrue(response.getLocation().toString().endsWith("/institution/" + institutionKey));
  }

  @Test
  public void unknownIdentifier() {
    WebResource webResourceResolve =
        client.resource(
            "http://localhost:"
                + RegistryServer.getPort()
                + "/grscicoll/resolve/dev.grbio.org/cool/foo");

    ClientResponse response = webResourceResolve.get(ClientResponse.class);

    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
}
