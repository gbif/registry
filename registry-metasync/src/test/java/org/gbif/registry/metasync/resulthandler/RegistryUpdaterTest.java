package org.gbif.registry.metasync.resulthandler;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.MetasyncHistoryService;
import org.gbif.api.vocabulary.EndpointType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.registry.metasync.api.SyncResult;
import org.gbif.registry.metasync.protocols.HttpGetMatcher;
import org.gbif.registry.metasync.protocols.tapir.TapirMetadataSynchroniser;
import org.gbif.registry.metasync.util.Constants;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RegistryUpdaterTest {

  private static TapirMetadataSynchroniser synchroniser;

  @Mock
  private DatasetService datasetService;
  @Mock
  private MetasyncHistoryService metasyncHistoryService;
  private RegistryUpdater updater;
  private Dataset dataset;
  private Installation installation;
  private Endpoint endpoint;

  @BeforeClass
  public static void init() throws IOException {
    HttpClient client = mock(HttpClient.class);
    // mock endpoint responses
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr?op=capabilities")))).thenReturn(
      prepareResponse(200, "tapir/capabilities1.xml"));
    when(client.execute(argThat(HttpGetMatcher.matchUrl("http://localhost/nmr")))).thenReturn(
      prepareResponse(200, "tapir/metadata1.xml"));
    when(
      client.execute(argThat(HttpGetMatcher
        .matchUrl("http://localhost/nmr?op=s&t=http%3A%2F%2Frs.gbif.org%2Ftemplates%2Ftapir%2Fdwc%2F1.4%2Fsci_name_range.xml&count=true&start=0&limit=1&lower=AAA&upper=zzz"))))
      .thenReturn(
        prepareResponse(200, "tapir/search1.xml"));
    synchroniser = new TapirMetadataSynchroniser(client);
  }

  @Before
  public void setup() {
    // same Endpoint for Installation and Dataset
    endpoint = new Endpoint();
    endpoint.setKey(1);
    endpoint.setUrl(URI.create("http://localhost/nmr"));

    // Installation, synchronized against
    installation = new Installation();
    installation.setType(InstallationType.TAPIR_INSTALLATION);
    installation.addEndpoint(endpoint);

    // Populated Dataset
    dataset = prepareDataset();

    // RegistryUpdater, using mocked web service client implementation
    updater = new RegistryUpdater(datasetService, metasyncHistoryService);
  }

  /**
   * Synchronize a TAPIR installation, updating a single existing Dataset. The HTTP responses, and the Registry web
   * service client are mocked.
   * </br>
   * The test verifies that the Dataset update was successful, counting the number of web service method invocations.
   * For example, if datasetService.addContact was called twice, and 2 contacts were expected to be persisted on
   * update, we can assume the service does its job correctly and just assert that the addContact() was called twice.
   */
  @Test
  public void testSaveUpdatedDatasets() throws Exception {
    // generate SyncResult, including a single updated Dataset
    SyncResult syncResult = synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));

    // update Dataset in Registry
    updater.saveUpdatedDatasets(syncResult);

    // update dataset 1 time
    verify(updater.getDatasetService(), times(1)).update(any(Dataset.class));

    // delete 1 existing machine tag, add 2 new
    verify(updater.getDatasetService(), times(1)).deleteMachineTag(any(UUID.class), anyInt());
    verify(updater.getDatasetService(), times(2)).addMachineTag(any(UUID.class), any(MachineTag.class));

    // delete 1 existing contact, add 2 new
    verify(updater.getDatasetService(), times(1)).deleteContact(any(UUID.class), anyInt());
    verify(updater.getDatasetService(), times(2)).addContact(any(UUID.class), any(Contact.class));

    // delete 1 endpoint, add 1 endpoint
    verify(updater.getDatasetService(), times(1)).deleteEndpoint(any(UUID.class), anyInt());
    verify(updater.getDatasetService(), times(1)).addEndpoint(any(UUID.class), any(Endpoint.class));

    // delete 0 identifiers, add 0 identifiers
    verify(updater.getDatasetService(), times(0)).deleteIdentifier(any(UUID.class), anyInt());
    verify(updater.getDatasetService(), times(0)).addIdentifier(any(UUID.class), any(Identifier.class));

    // delete 0 tags, add 0 tags
    verify(updater.getDatasetService(), times(0)).deleteTag(any(UUID.class), anyInt());
    verify(updater.getDatasetService(), times(0)).addTag(any(UUID.class), any(Tag.class));
  }

  /**
   * Synchronize a TAPIR installation, whose single existing Dataset has been migrated to DwC-A already. An endpoint of
   * type DwC-A is evidence of migration. Since the Dataset has been migrated, its metadata won't be updated from
   * TAPIR anymore.
   * </br>
   * The test verifies that the Dataset update was skipped, counting the number of web service method invocations.
   */
  @Test
  public void testSaveUpdatedDatasetsSkipped() throws Exception {
    Endpoint dwca = new Endpoint();
    dwca.setKey(100);
    dwca.setType(EndpointType.DWC_ARCHIVE);
    dwca.setUrl(URI.create("http://ipt.gbif.org/archive.do?r=test"));
    dataset.getEndpoints().add(dwca);

    // generate SyncResult, including a single updated Dataset
    SyncResult syncResult = synchroniser.syncInstallation(installation, Lists.newArrayList(dataset));

    // update Dataset in Registry
    updater.saveUpdatedDatasets(syncResult);

    // update dataset was skipped - service was called 0 times!
    verify(updater.getDatasetService(), times(0)).update(any(Dataset.class));
  }

  /**
   * Prepare TAPIR Dataset for use in tests, having 1 contact, 1 identifier, 2 machine tags, 1 tag, and 1 endpoint.
   */
  private Dataset prepareDataset() {
    // create TAPIR dataset with single endpoint
    Dataset dataset = new Dataset();
    dataset.setKey(UUID.randomUUID());
    dataset.setTitle("Beavers");
    dataset.addEndpoint(endpoint);

    // add single Contact
    Contact contact = new Contact();
    contact.setKey(1);
    contact.addEmail("test@gbif.org");
    dataset.setContacts(Lists.newArrayList(contact));

    // add single Identifier
    Identifier identifier = new Identifier();
    identifier.setKey(1);
    identifier.setType(IdentifierType.GBIF_PORTAL);
    identifier.setIdentifier("450");
    dataset.setIdentifiers(Lists.newArrayList(identifier));

    // add 2 MachineTags 1 with metasync.gbif.org namespace, and another not having it
    List<MachineTag> machineTags = Lists.newArrayList();

    MachineTag machineTag = new MachineTag();
    machineTag.setKey(1);
    machineTag.setNamespace(Constants.METADATA_NAMESPACE);
    machineTag.setName(Constants.DECLARED_COUNT);
    machineTag.setValue("1000");
    machineTags.add(machineTag);

    MachineTag machineTag2 = new MachineTag();
    machineTag2.setKey(2);
    machineTag2.setNamespace("public");
    machineTag2.setName("IsoCountryCode");
    machineTag2.setValue("DK");
    machineTags.add(machineTag2);

    dataset.setMachineTags(machineTags);

    // add 1 Tag
    Tag tag = new Tag();
    tag.setKey(1);
    tag.setValue("Invasive");
    dataset.setTags(Lists.newArrayList(tag));

    return dataset;
  }

  private static HttpResponse prepareResponse(int responseStatus, String fileName) throws IOException {
    HttpResponse response =
      new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), responseStatus, ""));
    response.setStatusCode(responseStatus);
    byte[] bytes = Resources.toByteArray(Resources.getResource(fileName));
    response.setEntity(new ByteArrayEntity(bytes));
    return response;
  }
}
