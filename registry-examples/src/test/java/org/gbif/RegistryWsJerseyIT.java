package org.gbif;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.InstallationType;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.gbif.config.jersey.RegistryWsClientFactoryJersey.datasetService;
import static org.gbif.config.jersey.RegistryWsClientFactoryJersey.datasetServiceReadOnly;
import static org.gbif.config.jersey.RegistryWsClientFactoryJersey.installationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore("This tests uses registry-sandbox which doesn't use the latest registry api")
public class RegistryWsJerseyIT {
  private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(RegistryWsJerseyIT.class);
  private static final String DATASET_TITLE = "Dataset - Ws Client Demo";

  private static UUID datasetKey;
  private static UUID installationKey;

  /**
   * Create Dataset one time only.
   * </p>
   * Since nodes and organizations can't be created using the API, an existing organization (endorsed already) is
   * used as both the publishing organization for the dataset, and the hosting organization for the installation.
   */
  @BeforeClass
  public static void createDataset() throws URISyntaxException {
    // Publishing and hosting organization: Test Organization #1, endorsed by DanBIF
    UUID organizationKey = UUID.fromString("0a16da09-7719-40de-8d4f-56a15ed52fb6");

    // create new installation, indicating which organization is hosting the installation
    Installation installation = new Installation();
    installation.setTitle("Installation - Ws Client Demo");
    installation.setType(InstallationType.IPT_INSTALLATION);
    installation.setDisabled(false);
    installation.setDescription("Test IPT instance created using GBIF Registry ws client");
    installation.setOrganizationKey(organizationKey);
    installationKey = installationService().create(installation);

    // create new dataset, indicating which organization owns the dataset, and which installation hosts the dataset
    Dataset dataset = new Dataset();
    dataset.setTitle(DATASET_TITLE);
    dataset.setType(DatasetType.OCCURRENCE);
    dataset.setDescription("Test Dataset created using GBIF Registry ws client");
    dataset.setPublishingOrganizationKey(organizationKey);
    dataset.setInstallationKey(installationKey);
    datasetKey = datasetService().create(dataset);
    LOG.info("Dataset created with key: {}", datasetKey);
  }

  /**
   * Read dataset
   */
  @Test
  public void testReadDataset() {
    Dataset dataset = datasetServiceReadOnly().get(datasetKey);
    assertEquals(DATASET_TITLE, dataset.getTitle());
  }

  /**
   * Update dataset, add an enpoint.
   */
  @Test
  public void testAddEndpoint() throws URISyntaxException {
    Endpoint endpoint = new Endpoint();
    endpoint.setUrl(new URI("http://ipt.gbif.org/archive.do?r=diptera"));
    endpoint.setType(EndpointType.DWC_ARCHIVE);
    datasetService().addEndpoint(datasetKey, endpoint);
  }

  /**
   * Update dataset: add a contact.
   */
  @Test
  public void testAddContact() throws URISyntaxException {
    Contact contact = new Contact();
    contact.setFirstName("Jose");
    contact.setLastName("Cuadra");
    contact.setEmail(Lists.newArrayList("jcuadra@abc.org"));
    contact.setType(ContactType.ADMINISTRATIVE_POINT_OF_CONTACT);
    datasetService().addContact(datasetKey, contact);
  }

  /**
   * Delete dataset, still exists but deleted flag has been set.
   */
  @Test
  public void testDeleteDataset() {
    datasetService().delete(datasetKey);
    Dataset dataset = datasetServiceReadOnly().get(datasetKey);
    assertNotNull(dataset.getDeleted());
  }

  /**
   * Delete installation, still exists but deleted flag has been set.
   */
  @Test
  public void testDeleteInstallation() {
    installationService().delete(installationKey);
    Installation installation = installationService().get(installationKey);
    assertNotNull(installation.getDeleted());
  }
}
