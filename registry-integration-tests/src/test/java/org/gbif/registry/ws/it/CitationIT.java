package org.gbif.registry.ws.it;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.domain.ws.Citation;
import org.gbif.registry.domain.ws.CitationCreationRequest;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.registry.ws.resources.CitationResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CitationIT extends BaseItTest {

  private static final PagingRequest REGULAR_PAGE = new PagingRequest();

  private final CitationResource citationResource;
  private final TestDataFactory testDataFactory;
  private final NodeResource nodeService;
  private final OrganizationResource organizationService;
  private final InstallationResource installationService;

  @Autowired
  public CitationIT(
      CitationResource citationResource,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      TestDataFactory testDataFactory,
      NodeResource nodeService,
      OrganizationResource organizationService,
      InstallationResource installationService) {
    super(simplePrincipalProvider, esServer);
    this.citationResource = citationResource;
    this.testDataFactory = testDataFactory;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
  }

  @Test
  public void testCreateCitation() {
    CitationCreationRequest requestData = new CitationCreationRequest();
    requestData.setTitle("Let's test citation");
    requestData.setOriginalDownloadDOI(new DOI("10.21373/55555"));
    requestData.setCreator("it");
    requestData.setTarget(URI.create("https://www.gbif.org"));
    requestData.setRelatedDatasets(Arrays.asList(UUID.randomUUID().toString(), new DOI("10.21373/44444").toString()));

    Citation citation = citationResource.createCitation(requestData);

    Citation actual = citationResource.getCitation(citation.getDoi().getPrefix(), citation.getDoi().getSuffix());

    assertNotNull(actual);
    assertEquals(citation.getDoi(), actual.getDoi());
    assertEquals(requestData.getOriginalDownloadDOI(), actual.getOriginalDownloadDOI());
    assertEquals(requestData.getTarget(), actual.getTarget());
    assertEquals(requestData.getTitle(), actual.getTitle());
    assertNotNull(actual.getCreated());
    assertNotNull(actual.getCreatedBy());
    assertNotNull(actual.getModified());
    assertNotNull(actual.getModifiedBy());
  }

  @Test
  public void testDatasetCitation() {
    DOI originalDownloadDoi = new DOI("10.21373/55555");

    Dataset firstDataset = newDataset();
    Dataset secondDataset = newDataset();
    Dataset thirdDataset = newDataset();

    CitationCreationRequest requestData1 = newCitationRequest(
        originalDownloadDoi,
        Arrays.asList(secondDataset.getKey().toString(), firstDataset.getDoi().getDoiName()));

    CitationCreationRequest requestData2 = newCitationRequest(
        originalDownloadDoi,
        Arrays.asList(secondDataset.getDoi().getDoiName(), firstDataset.getKey().toString()));

    CitationCreationRequest requestData3 = newCitationRequest(
        originalDownloadDoi,
        Collections.singletonList(thirdDataset.getDoi().getDoiName()));

    Citation citation1 = citationResource.createCitation(requestData1);
    Citation citation2 = citationResource.createCitation(requestData2);
    Citation citation3 = citationResource.createCitation(requestData3);

    PagingResponse<Dataset> citationDatasetsPage1 =
        citationResource.getCitationDatasets(citation1.getDoi(), REGULAR_PAGE);
    assertNotNull(citationDatasetsPage1);
    assertEquals(2, citationDatasetsPage1.getCount());

    PagingResponse<Dataset> citationDatasetsPage2 =
        citationResource.getCitationDatasets(citation3.getDoi(), REGULAR_PAGE);
    assertNotNull(citationDatasetsPage2);
    assertEquals(1, citationDatasetsPage2.getCount());

    PagingResponse<Citation> datasetCitationPage1 =
        citationResource.getDatasetCitation(firstDataset.getKey(), REGULAR_PAGE);
    assertNotNull(datasetCitationPage1);
    assertEquals(2, datasetCitationPage1.getCount());

    PagingResponse<Citation> datasetCitationPage2 =
        citationResource.getDatasetCitation(thirdDataset.getKey(), REGULAR_PAGE);
    assertNotNull(datasetCitationPage2);
    assertEquals(1, datasetCitationPage2.getCount());
  }

  private CitationCreationRequest newCitationRequest(DOI originalDownloadDOI, List<String> relatedDatasets) {
    CitationCreationRequest creationRequest = new CitationCreationRequest();
    creationRequest.setTitle("Let's test citation");
    creationRequest.setOriginalDownloadDOI(originalDownloadDOI);
    creationRequest.setCreator("it");
    creationRequest.setTarget(URI.create("https://www.gbif.org"));
    creationRequest.setRelatedDatasets(relatedDatasets);

    return creationRequest;
  }

  private Dataset newDataset() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(testDataFactory.newNode());
    // publishing organization (required field)
    Organization o = testDataFactory.newOrganization(nodeKey);
    UUID organizationKey = organizationService.create(o);

    Installation i = testDataFactory.newInstallation(organizationKey);
    UUID installationKey = installationService.create(i);

    return testDataFactory.newPersistedDataset(organizationKey, installationKey);
  }
}
