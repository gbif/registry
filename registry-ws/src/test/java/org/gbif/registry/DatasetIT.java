/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.model.registry.search.DatasetSearchResult;
import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.search.DatasetIndexUpdateListener;
import org.gbif.registry.search.DatasetSearchUpdateUtils;
import org.gbif.registry.search.SolrInitializer;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;
import org.gbif.registry.ws.resources.DatasetResource;
import org.gbif.registry.ws.resources.InstallationResource;
import org.gbif.registry.ws.resources.NodeResource;
import org.gbif.registry.ws.resources.OrganizationResource;
import org.gbif.utils.file.FileUtils;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.ValidationException;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.apache.solr.client.solrj.SolrServer;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * This is parameterized to run the same test routines for the following:
 * <ol>
 * <li>The persistence layer</li>
 * <li>The WS service layer</li>
 * <li>The WS service client layer</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class DatasetIT extends NetworkEntityTest<Dataset> {

  // Resets SOLR between each method
  @Rule
  public final SolrInitializer solrRule;

  private final DatasetService service;
  private final DatasetSearchService searchService;
  private final OrganizationService organizationService;
  private final NodeService nodeService;
  private final InstallationService installationService;
  private final DatasetIndexUpdateListener datasetIndexUpdater;

  @Parameters
  public static Iterable<Object[]> data() {
    final Injector client = webserviceClient();
    final Injector webservice = webservice();
    return ImmutableList.<Object[]>of(
      new Object[] {
        webservice.getInstance(DatasetResource.class),
        webservice.getInstance(DatasetResource.class),
        webservice.getInstance(OrganizationResource.class),
        webservice.getInstance(NodeResource.class),
        webservice.getInstance(InstallationResource.class),
        webservice.getInstance(Key.get(SolrServer.class, Names.named("Dataset"))),
        webservice.getInstance(DatasetIndexUpdateListener.class),
        null // SimplePrincipalProvider only set in web service client
      }
      ,
      new Object[] {
        client.getInstance(DatasetService.class),
        client.getInstance(DatasetSearchService.class),
        client.getInstance(OrganizationService.class),
        client.getInstance(NodeService.class),
        client.getInstance(InstallationService.class),
        null, // use the SOLR in Grizzly
        null, // use the SOLR in Grizzly
        client.getInstance(SimplePrincipalProvider.class)
      }
      );
  }

  public DatasetIT(
    DatasetService service,
    DatasetSearchService searchService,
    OrganizationService organizationService,
    NodeService nodeService,
    InstallationService installationService,
    @Nullable SolrServer solrServer,
    @Nullable DatasetIndexUpdateListener datasetIndexUpdater,
    @Nullable SimplePrincipalProvider pp) {
    super(service, pp);
    this.service = service;
    this.searchService = searchService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    // if no SOLR are given for the test, use the SOLR in Grizzly
    solrServer = solrServer == null ? RegistryServer.INSTANCE.getSolrServer() : solrServer;
    this.datasetIndexUpdater =
      datasetIndexUpdater == null ? RegistryServer.INSTANCE.getDatasetUpdater() : datasetIndexUpdater;

    this.solrRule = new SolrInitializer(solrServer, this.datasetIndexUpdater);
  }

  @Test
  public void testConstituents() {
    Dataset parent = create(newEntity(), 1);
    for (int id = 0; id < 10; id++) {
      Dataset constituent = newEntity();
      constituent.setParentDatasetKey(parent.getKey());
      constituent.setType(parent.getType());
      create(constituent, id + 2);
    }

    assertEquals(10, service.get(parent.getKey()).getNumConstituents());
  }

  // Easier to test this here than OrganizationIT due to our utility dataset factory
  @Test
  public void testHostedByList() {
    Dataset dataset = create(newEntity(), 1);
    Installation i = installationService.get(dataset.getInstallationKey());
    assertNotNull("Dataset should have an installation", i);
    PagingResponse<Dataset> published =
      organizationService.publishedDatasets(i.getOrganizationKey(), new PagingRequest());
    PagingResponse<Dataset> hosted = organizationService.hostedDatasets(i.getOrganizationKey(), new PagingRequest());
    assertEquals("This installation should have only 1 published dataset", 1, published.getResults().size());
    assertTrue("This organization should not have any hosted datasets", hosted.getResults().isEmpty());
  }

  // Easier to test this here than OrganizationIT due to our utility dataset factory
  @Test
  public void testPublishedByList() {
    Dataset dataset = create(newEntity(), 1);
    PagingResponse<Dataset> published =
      organizationService.publishedDatasets(dataset.getPublishingOrganizationKey(), new PagingRequest());
    assertEquals("The organization should have only 1 dataset", 1, published.getResults().size());
    assertEquals("The organization should publish the dataset created", published.getResults().get(0).getKey(),
      dataset.getKey());

    assertEquals("The organization should have 1 dataset count", 1,
      organizationService.get(dataset.getPublishingOrganizationKey()).getNumPublishedDatasets());
  }

  // Easier to test this here than InstallationIT due to our utility dataset factory
  @Test
  public void testHostedByInstallationList() {
    Dataset dataset = create(newEntity(), 1);
    Installation i = installationService.get(dataset.getInstallationKey());
    assertNotNull("Dataset should have an installation", i);
    PagingResponse<Dataset> hosted =
      installationService.getHostedDatasets(dataset.getInstallationKey(), new PagingRequest());
    assertEquals("This installation should have only 1 hosted dataset", 1, hosted.getResults().size());
    assertEquals("Paging response counts are not being set", Long.valueOf(1), hosted.getCount());
    assertEquals("The hosted installation should serve the dataset created", hosted.getResults().get(0).getKey(),
      dataset.getKey());
  }

  @Test
  public void testTypedSearch() {
    Dataset d = newEntity();
    d.setType(DatasetType.CHECKLIST);
    d = create(d, 1);
    assertSearch(d.getTitle(), 1); // 1 result expected for a simple search

    DatasetSearchRequest req = new DatasetSearchRequest();
    req.addTypeFilter(DatasetType.CHECKLIST);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(1),
      resp.getCount());
  }

  @Test
  public void testSearchListener() {
    Dataset d = newEntity();
    d = create(d, 1);
    assertSearch(d.getTitle(), 1); // 1 result expected

    // update
    String oldTitle = d.getTitle();
    d.setTitle("NEW-DATASET-TITLE");
    service.update(d);
    assertSearch("*", 1);
    assertSearch(oldTitle, 0);
    assertSearch(d.getTitle(), 1);

    // update publishing organization title should be captured
    Organization publisher = organizationService.get(d.getPublishingOrganizationKey());
    assertSearch(publisher.getTitle(), 1);
    oldTitle = publisher.getTitle();
    publisher.setTitle("NEW-OWNER-TITLE");
    organizationService.update(publisher);
    assertSearch(oldTitle, 0);
    assertSearch(publisher.getTitle(), 1);

    // update hosting organization title should be captured
    Installation installation = installationService.get(d.getInstallationKey());
    assertNotNull("Installation should be present", installation);
    Organization host = organizationService.get(installation.getOrganizationKey());
    assertSearch(host.getTitle(), 1);
    oldTitle = host.getTitle();
    host.setTitle("NEW-HOST-TITLE");
    organizationService.update(host);
    assertSearch(oldTitle, 0);
    assertSearch(host.getTitle(), 1);

    // check a deletion removes the dataset for search
    service.delete(d.getKey());
    assertSearch(host.getTitle(), 0);
  }

  @Test
  public void testInstallationMove() {
    Dataset d = newEntity();
    d = create(d, 1);
    assertSearch(d.getTitle(), 1); // 1 result expected

    UUID nodeKey = nodeService.create(Nodes.newInstance());
    Organization o = Organizations.newInstance(nodeKey);
    o.setTitle("A-NEW-ORG");
    UUID organizationKey = organizationService.create(o);
    assertSearch(o.getTitle(), 0); // No datasets hosted by that organization yet

    Installation installation = installationService.get(d.getInstallationKey());
    installation.setOrganizationKey(organizationKey);
    installationService.update(installation); // we just moved the installation to a new organization

    assertSearch(o.getTitle(), 1); // The dataset moved with the organization!
    assertSearch("*", 1);
  }

  /**
   * Utility to verify that after waiting for SOLR to update, the given query returns the expected count of results.
   */
  private void assertSearch(String query, int expected) {
    DatasetSearchUpdateUtils.awaitUpdates(datasetIndexUpdater); // SOLR updates are asynchronous
    DatasetSearchRequest req = new DatasetSearchRequest();
    req.setQ(query);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    assertEquals("SOLR does not have the expected number of results for query[" + query + "]", Long.valueOf(expected),
      resp.getCount());
  }

  /**
   * Utility to verify that after waiting for SOLR to update, the given query returns the expected count of results.
   */
  private void assertSearch(Country publishingCountry, Country country, int expected) {
    DatasetSearchUpdateUtils.awaitUpdates(datasetIndexUpdater); // SOLR updates are asynchronous
    DatasetSearchRequest req = new DatasetSearchRequest();
    if (country != null) {
      req.addCountryFilter(country);
    }
    if (publishingCountry != null) {
      req.addPublishingCountryFilter(publishingCountry);
    }
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    assertEquals("SOLR does not have the expected number of results for country[" + country +
      "] and publishingCountry[" + publishingCountry + "]", Long.valueOf(expected),
      resp.getCount());
  }

  @Override
  protected Dataset newEntity() {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(Nodes.newInstance());
    // publishing organization (required field)
    Organization o = Organizations.newInstance(nodeKey);
    UUID organizationKey = organizationService.create(o);

    Installation i = Installations.newInstance(organizationKey);
    UUID installationKey = installationService.create(i);

    return newEntity(organizationKey, installationKey);
  }

  private Dataset newEntity(UUID organizationKey, UUID installationKey) {
    // the dataset
    Dataset d = Datasets.newInstance(organizationKey, installationKey);
    return d;
  }

  @Test
  public void testCitation() {
    Dataset dataset = create(newEntity(), 1);
    dataset = service.get(dataset.getKey());
    assertNotNull("Citation should never be null", dataset.getCitation());
    assertEquals("ABC", dataset.getCitation().getIdentifier());
    assertEquals("This is a citation text", dataset.getCitation().getText());

    // update it
    dataset.getCitation().setIdentifier("doi:123");
    dataset.getCitation().setText("GOD publishing, volume 123");
    service.update(dataset);
    dataset = service.get(dataset.getKey());
    assertEquals("doi:123", dataset.getCitation().getIdentifier());
    assertEquals("GOD publishing, volume 123", dataset.getCitation().getText());

    // setting to null should make it the default using the org:dataset titles
    dataset.getCitation().setText(null);
    service.update(dataset);
    dataset = service.get(dataset.getKey());
    assertEquals("doi:123", dataset.getCitation().getIdentifier());
    assertEquals("The BGBM: Pontaurus needs more than 255 characters for it's title. It is a very, very, very, very long title in the German language. Word by word and character by character it's exact title is: \"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\"", dataset.getCitation().getText());
  }

  @Test
  public void test404() throws IOException {
    assertNull(service.get(UUID.randomUUID()));
  }

  @Test
  public void testMetadata() throws IOException {
    Dataset d1 = create(newEntity(), 1);

    // upload a valid EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));

    // verify our dataset has changed
    Dataset d2 = service.get(d1.getKey());
    assertNotEquals("Dataset should have changed after metadata was uploaded", d1, d2);
    assertEquals("Tanzanian Entomological Collection", d2.getTitle());
    assertEquals("Created data should not change", d1.getCreated(), d2.getCreated());
    assertTrue("Dataset modification date should change", d1.getModified().before(d2.getModified()));

    // check metadata
    List<Metadata> metadata = service.listMetadata(d1.getKey(), MetadataType.DC);
    assertTrue("No Dublin Core uplaoded yet", metadata.isEmpty());

    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals("Exactly one EML uploaded", 1, metadata.size());
    assertEquals("Wrong metadata type", MetadataType.EML, metadata.get(0).getType());

    // upload subsequent DC document which has less priority than the previous EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/worms_dc.xml"));
    // verify our dataset has not changed
    Dataset d3 = service.get(d1.getKey());
    assertEquals("Tanzanian Entomological Collection", d3.getTitle());
    assertEquals("Created data should not change", d1.getCreated(), d3.getCreated());
  }

  @Test
  public void testByCountry() {
    createCountryDatasets(DatasetType.OCCURRENCE, Country.ANDORRA, 3);
    createCountryDatasets(DatasetType.OCCURRENCE, Country.DJIBOUTI, 1);
    createCountryDatasets(DatasetType.METADATA, Country.HAITI, 7);
    createCountryDatasets(DatasetType.OCCURRENCE, Country.HAITI, 3);
    createCountryDatasets(DatasetType.CHECKLIST, Country.HAITI, 2);

    assertResultsOfSize(service.listByCountry(Country.UNKNOWN, null, new PagingRequest()), 0);
    assertResultsOfSize(service.listByCountry(Country.ANDORRA, null, new PagingRequest()), 3);
    assertResultsOfSize(service.listByCountry(Country.DJIBOUTI, null, new PagingRequest()), 1);
    assertResultsOfSize(service.listByCountry(Country.HAITI, null, new PagingRequest()), 12);

    assertResultsOfSize(service.listByCountry(Country.ANDORRA, DatasetType.CHECKLIST, new PagingRequest()), 0);
    assertResultsOfSize(service.listByCountry(Country.HAITI, DatasetType.OCCURRENCE, new PagingRequest()), 3);
    assertResultsOfSize(service.listByCountry(Country.HAITI, DatasetType.CHECKLIST, new PagingRequest()), 2);
    assertResultsOfSize(service.listByCountry(Country.HAITI, DatasetType.METADATA, new PagingRequest()), 7);
  }

  @Test
  public void testListByType() {
    createDatasetsWithType(DatasetType.METADATA, 1);
    createDatasetsWithType(DatasetType.CHECKLIST, 2);
    createDatasetsWithType(DatasetType.OCCURRENCE, 3);

    assertResultsOfSize(service.listByType(DatasetType.METADATA, new PagingRequest()), 1);
    assertResultsOfSize(service.listByType(DatasetType.CHECKLIST, new PagingRequest()), 2);
    assertResultsOfSize(service.listByType(DatasetType.OCCURRENCE, new PagingRequest()), 3);
  }

  @Test
  @Ignore("Country coverage not populated yet: http://dev.gbif.org/issues/browse/REG-393")
  public void testCountrySearch() {
    createCountryDatasets(Country.ANDORRA, 3);
    createCountryDatasets(DatasetType.OCCURRENCE, Country.DJIBOUTI, 1, Country.DJIBOUTI);
    createCountryDatasets(DatasetType.OCCURRENCE, Country.HAITI, 3, Country.AFGHANISTAN, Country.DENMARK);
    createCountryDatasets(DatasetType.CHECKLIST, Country.HAITI, 4, Country.GABON, Country.FIJI);
    createCountryDatasets(DatasetType.OCCURRENCE, Country.DOMINICA, 2, Country.DJIBOUTI);

    assertSearch(Country.ALBANIA, null, 0);
    assertSearch(Country.ANDORRA, null, 3);
    assertSearch(Country.DJIBOUTI, null, 1);
    assertSearch(Country.HAITI, null, 7);
    assertSearch(Country.UNKNOWN, null, 0);

    assertSearch(Country.HAITI, Country.GABON, 4);
    assertSearch(Country.HAITI, Country.FIJI, 4);
    assertSearch(Country.HAITI, Country.DENMARK, 3);
    assertSearch(Country.DJIBOUTI, Country.DENMARK, 0);
    assertSearch(Country.DJIBOUTI, Country.DJIBOUTI, 1);
    assertSearch(Country.DJIBOUTI, Country.GERMANY, 0);
    assertSearch(null, Country.DJIBOUTI, 3);
  }

  @Test(expected = ValidationException.class)
  public void createDatasetsWithInvalidUri() {
    Dataset d = newEntity();
    d.setHomepage(URI.create("file:/test.txt"));
    service.create(d);
  }

  private void createCountryDatasets(Country publishingCountry, int number) {
    createCountryDatasets(DatasetType.OCCURRENCE, publishingCountry, number, (Country) null);
  }

  private void createCountryDatasets(DatasetType type, Country publishingCountry, int number, Country... countries) {
    Dataset d = addCountryCoverage(newEntity(), countries);
    d.setType(type);
    service.create(d);

    // assign a controlled country based organization
    Organization org = organizationService.get(d.getPublishingOrganizationKey());
    org.setCountry(publishingCountry);
    organizationService.update(org);

    // create datasets for it
    for (int x = 1; x < number; x++) {
      d = addCountryCoverage(newEntity(org.getKey(), d.getInstallationKey()));
      d.setType(type);
      service.create(d);
    }
  }

  /**
   * Create a number of new Datasets, having a particular dataset type.
   * 
   * @param type dataset type
   * @param number amount of datasets to create
   */
  private void createDatasetsWithType(DatasetType type, int number) {
    // create datasets for it
    for (int x = 0; x < number; x++) {
      Dataset d = newEntity();
      d.setType(type);
      service.create(d);
    }
  }

  private Dataset addCountryCoverage(Dataset d, Country... countries) {
    if (countries != null) {
      for (Country c : countries) {
        if (c != null) {
          d.getCountryCoverage().add(c);
        }
      }
    }
    return d;
  }
}
