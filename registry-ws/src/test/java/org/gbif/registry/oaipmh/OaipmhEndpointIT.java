package org.gbif.registry.oaipmh;

import org.gbif.api.service.registry.OccurrenceDownloadService;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.OccurrenceDownloadResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import org.dspace.xoai.model.oaipmh.Record;
import org.dspace.xoai.serviceprovider.ServiceProvider;
import org.dspace.xoai.serviceprovider.client.HttpOAIClient;
import org.dspace.xoai.serviceprovider.client.OAIClient;
import org.dspace.xoai.serviceprovider.exceptions.CannotDisseminateFormatException;
import org.dspace.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import org.dspace.xoai.serviceprovider.model.Context;
import org.dspace.xoai.serviceprovider.parameters.GetRecordParameters;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;

/**
 * Test the OAI-PMH endpoint using the XOAI OAI-PMH client library.
 */
@RunWith(Parameterized.class)
public class OaipmhEndpointIT {

  String BASE_URL_FORMAT = "http://localhost:%d/oaipmh";
  String EML_FORMAT = "eml";

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  @ClassRule
  public static final RegistryServer registryServer = RegistryServer.INSTANCE;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  private final OccurrenceDownloadService occurrenceDownloadService;
  private final SimplePrincipalProvider simplePrincipalProvider;

  private final ServiceProvider serviceProvider;

  public OaipmhEndpointIT(
          OccurrenceDownloadService occurrenceDownloadService,
          SimplePrincipalProvider simplePrincipalProvider) {
    this.occurrenceDownloadService = occurrenceDownloadService;
    this.simplePrincipalProvider = simplePrincipalProvider;

    OAIClient oaiClient = new HttpOAIClient(String.format(BASE_URL_FORMAT, registryServer.getPort()));
    Context context = new Context().withOAIClient(oaiClient);
    serviceProvider = new ServiceProvider(context);
  }

  @org.junit.runners.Parameterized.Parameters
  public static Iterable<Object[]> data() {
    final Injector webservice = webservice();
    final Injector client = webserviceClient();

    return ImmutableList.<Object[]>of(
            new Object[] {webservice.getInstance(OccurrenceDownloadResource.class), null},
            new Object[] {client.getInstance(OccurrenceDownloadService.class), client.getInstance(SimplePrincipalProvider.class)}
    );
  }

  @Test(expected = IdDoesNotExistException.class)
  public void getRecord_notFound() throws Throwable {

    serviceProvider.getRecord(
            GetRecordParameters.request()
                    .withIdentifier("non-existent-record-identifier")
                    .withMetadataFormatPrefix(EML_FORMAT)
    );
  }

  @Test(expected = IdDoesNotExistException.class)
  // TODO: Add data. @Test(expected = CannotDisseminateFormatException.class)
  public void getRecord_unsuppertedMetadataFormat () throws Exception {
    serviceProvider.getRecord(GetRecordParameters.request().withIdentifier("TODO-add-a-record").withMetadataFormatPrefix("made-up-metadata-format"));
  }

  @Test(expected = IdDoesNotExistException.class)
  // TODO: Add data. @Test
  public void getRecord_found() throws Throwable {

    Record record = serviceProvider.getRecord(
            GetRecordParameters.request()
                    .withIdentifier("TODO-add-a-record")
                    .withMetadataFormatPrefix(EML_FORMAT)
    );

    assertEquals("TODO-add-a-record", record.getHeader().getIdentifier());
  }

}
