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

import org.apache.commons.beanutils.BeanUtils;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.common.search.SearchResponse;
import org.gbif.api.model.registry.Citation;
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
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.grizzly.RegistryServer;
import org.gbif.registry.search.DatasetIndexService;
import org.gbif.registry.search.DatasetSearchUpdateUtils;
import org.gbif.registry.search.SolrInitializer;
import org.gbif.registry.search.guice.RegistrySearchModule;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.inject.Injector;
import org.apache.ibatis.io.Resources;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.gbif.registry.guice.RegistryTestModules.webservice;
import static org.gbif.registry.guice.RegistryTestModules.webserviceClient;
import static org.gbif.registry.utils.Datasets.buildExpectedProcessedProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
  private final DatasetIndexService datasetIndexService;

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
        webservice.getInstance(RegistrySearchModule.DATASET_KEY),
        webservice.getInstance(DatasetIndexService.class),
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
    @Nullable SolrClient solrClient,
    @Nullable DatasetIndexService indexService,
    @Nullable SimplePrincipalProvider pp) {
    super(service, pp);
    this.service = service;
    this.searchService = searchService;
    this.organizationService = organizationService;
    this.nodeService = nodeService;
    this.installationService = installationService;
    // if no SOLR are given for the test, use the SOLR in Grizzly
    solrClient = solrClient == null ? RegistryServer.INSTANCE.getSolrClient() : solrClient;
    this.datasetIndexService = indexService == null ? RegistryServer.INSTANCE.getIndexService() : indexService;
    solrRule = new SolrInitializer(solrClient, this.datasetIndexService);
  }

  @Test
  public void testCreateDoi() {
    Dataset d = newEntity();
    service.create(d);
    assertEquals(Datasets.DATASET_DOI, d.getDoi());

    d = newEntity();
    d.setDoi(null);
    UUID key = service.create(d);
    d = service.get(key);
    assertFalse(Datasets.DATASET_DOI.equals(d.getDoi()));
    assertEquals(DOI.TEST_PREFIX, d.getDoi().getPrefix());
  }

  /**
   * Override creation to add process properties.
   * @param orig
   * @param expectedCount
   * @return
   */
  @Override
  protected Dataset create(Dataset orig, int expectedCount) {
    return create(orig, expectedCount, buildExpectedProcessedProperties(orig));
  }

  protected Dataset duplicateForCreateAsEditorTest(Dataset entity) throws Exception {
    Dataset duplicate = (Dataset) BeanUtils.cloneBean(entity);
    duplicate.setPublishingOrganizationKey(entity.getPublishingOrganizationKey());
    duplicate.setInstallationKey(entity.getInstallationKey());
    return duplicate;
  }

  protected UUID keyForCreateAsEditorTest(Dataset entity) {
    return organizationService.get(entity.getPublishingOrganizationKey()).getEndorsingNodeKey();
  }

  @Test
  public void testConstituents() {
    Dataset parent = newAndCreate(1);
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
    Dataset dataset = newAndCreate(1);
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
    Dataset dataset = newAndCreate(1);
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
    Dataset dataset = newAndCreate(1);
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
  public void testMultiCountryFacet() {
    Dataset d = newEntity(Country.ALGERIA);
    d.setType(DatasetType.CHECKLIST);
    d = create(d, 1);

    d = newEntity(Country.GERMANY);
    d.setType(DatasetType.CHECKLIST);
    d = create(d, 2);

    d = newEntity(Country.FRANCE);
    d.setType(DatasetType.OCCURRENCE);
    d = create(d, 3);

    d = newEntity(Country.GHANA);
    d.setType(DatasetType.OCCURRENCE);
    d = create(d, 4);

    DatasetSearchRequest req = new DatasetSearchRequest();
    req.addPublishingCountryFilter(Country.ANGOLA);
    req.addFacets(DatasetSearchParameter.PUBLISHING_COUNTRY);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(0), resp.getCount());
    assertEquals("SOLR does not have the expected number of facets for query[" + req + "]", 1, resp.getFacets().size());
    assertEquals("SOLR does not have the expected number of facet values for query[" + req + "]", 0, resp.getFacets().get(0).getCounts().size());

    req = new DatasetSearchRequest();
    req.addPublishingCountryFilter(Country.ALGERIA);
    req.addFacets(DatasetSearchParameter.PUBLISHING_COUNTRY);
    req.addFacets(DatasetSearchParameter.LICENSE);
    resp = searchService.search(req);
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(1), resp.getCount());
    assertEquals("SOLR does not have the expected number of facets for query[" + req + "]", 2, resp.getFacets().size());

    req = new DatasetSearchRequest();
    req.addPublishingCountryFilter(Country.GERMANY);
    req.setMultiSelectFacets(true);
    req.addFacets(DatasetSearchParameter.PUBLISHING_COUNTRY);

    resp = searchService.search(req);
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(1), resp.getCount());
    assertEquals("SOLR does not have the expected number of facet values for query[" + req + "]", 4, resp.getFacets().get(0).getCounts().size());
  }

  @Test
  public void testSearchParameter() throws InterruptedException {
    Dataset d = newEntity(Country.SOUTH_AFRICA);
    d.setType(DatasetType.CHECKLIST);
    d.setLicense(License.CC0_1_0);
    d.setLanguage(Language.AFRIKAANS);
    create(d, 1);

    Thread.sleep(100);

    DatasetSearchRequest req = new DatasetSearchRequest();
    req.addPublishingCountryFilter(Country.GERMANY);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(0),
        resp.getCount());

    req.addPublishingCountryFilter(Country.SOUTH_AFRICA);
    req.addTypeFilter(DatasetType.CHECKLIST);
    resp = searchService.search(req);
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(1),
        resp.getCount());
  }

  @Test
  public void testSearchLargeTitles() {
    Dataset d = newEntity();
    d.setType(DatasetType.OCCURRENCE);
    d = create(d, 1);

    testSearch(d.getTitle());
    testSearch("Pontaurus needs more than 255 characters for it's title");
    testSearch("\"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\"");
    testSearch("Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei");
    testSearch("PonTaurus");
    testSearch("Pon Taurus");
    testSearch("Bolkar Daglari Aladaglari");
    testSearch("PonTaurus Bolkar Daglari Aladaglari");
  }

  @Test
  public void testEventTypeSearch() {
    Dataset d = newEntity();
    d.setType(DatasetType.SAMPLING_EVENT);
    d = create(d, 1);

    assertSearch(d.getTitle(), 1);

    DatasetSearchRequest req = new DatasetSearchRequest();
    req.addTypeFilter(DatasetType.SAMPLING_EVENT);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    assertEquals("SOLR does not have the expected number of results for query[" + req + "]", Long.valueOf(1),
      resp.getCount());
  }


  @Test
  @Ignore("See https://github.com/gbif/registry/issues/22")
  public void testDismaxSearch() throws InterruptedException {

    Dataset d = newAndCreate(1);
    final UUID pubKey = d.getPublishingOrganizationKey();
    final UUID instKey = d.getInstallationKey();
    final UUID nodeKey = organizationService.get(pubKey).getEndorsingNodeKey();

    d = new Dataset();
    d.setPublishingOrganizationKey(pubKey);
    d.setInstallationKey(instKey);
    d.setTitle("eBird is cool");
    d.setLicense(License.CC0_1_0);
    d.setType(DatasetType.CHECKLIST);
    d.setDescription("bli bla blub, mein Hund ist ins Klo gefallen. Oh je! Der kommt da alleine gar nicht mehr raus.");
    service.create(d);

    d.setKey(null);
    d.setType(DatasetType.OCCURRENCE);
    d.setTitle("Fall in eBird ");
    d.setDescription("bli bla blub, es gibt nix neues.");
    service.create(d);

    d.setKey(null);
    d.setTitle("Bird tracking - GPS tracking of Lesser Black-backed Gulls and Herring Gulls breeding at the southern North Sea coast");
    d.setDescription("Bird tracking - GPS tracking of Lesser Black-backed Gulls and Herring Gulls breeding at the southern North Sea coast is a species occurrence dataset published by the Research Institute for Nature and Forest (INBO) and described in Stienen et al. 2016 (http://doi.org/10.3897/zookeys.555.6173) The dataset contains close to 2.5 million occurrences, recorded by 101 GPS trackers mounted on 75 Lesser Black-backed Gulls and 26 Herring Gulls breeding at the Belgian and Dutch coast (see https://inbo.cartodb.com/u/lifewatch/viz/da04f120-ea70-11e4-a3f2-0e853d047bba/public_map for a visualization of the data). The trackers were developed by the University of Amsterdam Bird Tracking System (UvA-BiTS, http://www.uva-bits.nl). These automatically record and transmit bird movements, which allows us and others to study their habitat use and migration behaviour in great detail. Our bird tracking network is operational since 2013. It is funded for LifeWatch by the Hercules Foundation and maintained in collaboration with UvA-BiTS and the Flanders Marine Institute (VLIZ). The recorded data are periodically released in bulk as open data (http://dataset.inbo.be/bird-tracking-gull-occurrences), and are also accessible through CartoDB and the Global Biodiversity Information Facility (GBIF, http://doi.org/10.15468/02omly). See the dataset metadata for contact information, scope and methodology. Issues with the dataset can be reported at https://github.com/LifeWatchINBO/data-publication/tree/master/datasets/bird-tracking-gull-occurrences");
    final UUID gpsKey = service.create(d);

    d.setKey(null);
    d.setTitle("BID-AF2015-0004-NAC- données de Dénombrements Internationaux des Oiseaux d'Eau en Afrique");
    d.setDescription("Le Togo est un pays d’Afrique de l’Ouest limité à l’Est par le Bénin, à l’Ouest par le Ghana, au Nord par le Burkina-Faso et au sud par l’Océan Atlantique. Il est compris entre les latitudes 6° 06’ Sud et 11° 08’ Nord et les longitudes 0° 09’ Ouest et 1° 49’ Est avec une superficie de 56.600 km2. Cette configuration géographique lui confère une gradation climatique favorisée par l’existence d’importantes zones humides estimées à 2210 km2. En l’absence quasi-totale de leur plan de gestion, leur dégradation s’est accentuée au fils des années au risque d’entrainer à terme leur disparition si des mesures adéquates ne sont pas prises. Le Togo, Partie à un certain nombre de traités et accords relatifs à l’environnement et aux ressources forestières notamment à l’accord sur la conservation des oiseaux d’eau migrateurs (AEWA) a pris l’engagement d’enclencher le processus de gestion des zones humides du pays notamment les zones humides des bassins du Mono, du Zio et de Haho. Dans cette dynamique, la direction des ressources forestières participe annuellement aux recensements des oiseaux d’eau migrateurs. C’est dans ce contexte que cette opération de dénombrement s’est effectuée le 21 janvier 2016 grâce à l’appui de Wetlantlands International avec pour objectif de contribuer à l’actualisation de données mondiales de ces espèces. L’opération s’est déroulée uniquement au niveau des zones humides du littoral. Au point de vue approche méthodologique, le travail a été fait en trois phases. -\tLa première phase a consisté en une mission effectuée sur le terrain au cours de laquelle il a été procédé à l’identification des sites devant faire l’objet de décompte. -\tLa deuxième phase est axée sur le recensement des oiseaux d’eau sur les différents sites ; et -\tLa troisième phase est le rapportage prenant en compte l’ensemble des activités menées, les analyses technique et scientifique portant sur la notification et le suivi des espèces phares sur les sites. II - OBJECTIF 1.1 Objectif global Il s’agit de contribuer à la mise à jour de la Base de Données Internationales de Comptage (IWC) d’oiseaux d’eau. 1.2 Objectifs spécifiques A travers cette opération, il a été question de : \uF0A7\tCollecter les informations relatives aux caractéristiques écologiques des sites de décompte cibles; \uF0A7\tConduire les recensements des oiseaux d’eau en janvier 2016. II- METHODOLOGIE DE TRAVAIL ET MATERIEL UTILISE 2.1 Méthodologie de travail Dans le cadre de cette opération, une équipe a été mise en place pour une visite prospective des sites. Il s’agit d’un spécialiste de faune et d’un spécialiste en gestion des zones humides et d’un biologiste environnementaliste. Ceux-ci se sont rendus sur le terrain pour un pré-dénombrement. Cette étape a permis de faire l’inventaire qualitatif afin de s’assurer de la présence ou non des oiseaux d’eau migrateurs, objet de la mission et d’avoir une idée sur les caractéristiques écologiques des sites. Les résultats de ce pré-dénombrement ont conduit à la constitution de six équipes multidisciplinaires pour couvrir les six sites retenus. Il s’agit de biologistes, d’environnementalistes, de spécialistes en aménagement de la faune, de spécialistes en zone humide et de spécialistes en gestion des ressources forestières. Le dénombrement proprement dit a été effectué le 21 janvier 2016 avec la participation des acteurs indiqués dans le tableau I en annexe. Les intéressés se sont rendus dans les limites des zones ciblées pour compter les oiseaux. 2.2 Matériel utilisé La mise en œuvre des activités a nécessité les moyens ci-après: -\tUn véhicule 4 x 4 ; -\tDouze motos ; - Deux appareils de Système de Positionnement Géographique (GPS) pour la prise des coordonnées des sites lors de la visite prospective ; -\tKits de terrain (Bloc note; fiches de terrain, écritoires etc.…) pour la collecte des informations; - Trois appareils photos pour la prise des images ; -\tSix paires de jumelles pour l’observation et l’identification des oiseaux ; -\tCinq guides de terrain pour l’identification des espèces (les oiseaux de l’Ouest africain, les limicoles : comment les reconnaître et Birds of Southern Africa); -\tTrois ordinateurs portables pour la compilation et le traitement des données ainsi que pour le rapportage. III - RESULTATS 3. 1 Recensement général des oiseaux Le tableau II en excel annexé au présent rapport présente la synthèse des résultats du dénombrement effectué le 21 janvier 2016 au Togo. 3.2 Commentaire Au total six (06) sites de décompte ont été explorés dans le cadre de cette opération de dénombrement des oiseaux d’eau à cause des moyens insuffisants. Tous ces sites sont localisés au niveau des zones humides du Littoral. Il s’agit de deux sites au niveau de la lagune de Bè, du site du retenu d’eau de l’Université de Lomé, d’Agbalépédogan, de l’ancien Wharf et ses environs et de la mare Togo 2000. L’analyse des données de décompte indiquée dans le tableau II, montre une diversité des espèces au niveau des six sites. L’un des faits marquant de cette opération est la diversité importante observée au niveau du lac artificiel d’Agbalépédogan (LAA) où ont été dénombrées 19 espèces avec un effectif total de 150 individus. CONCLUSION ET RECOMMANDATIONS La participation du Togo aux décomptes des oiseaux d’eau, année 2016 prouve l’attachement du pays non seulement aux objectifs de l’accord sur la conservation des oiseaux d’eau migrateurs d’Afrique Eurasie (AEWA) mais également sa volonté à partager ses expériences avec les autres pays, Parties à cet accord en produisant des informations pour contribuer à l’actualisation de la base de données de comptages internationaux d’oiseaux (IWC). Dans cette dynamique, notre pays est toujours disposé à participer aux opérations de dénombrement pour les saisons à venir en prenant en compte les zones humiques importantes pour la conservation des oiseaux d’eau. Pour couvrir toute l’étendue du territoire national, il y a nécessité que les moyens alloués pour cette opération soient conséquents. Les observations sur le terrain indiquent une dégradation avancée des écosystèmes des zones humides du littoral et il y a nécessité d’inverser cette tendance pour assurer le cycle biologique des espèces à travers un vaste projet de restauration des sites concernés. A ce titre, le Togo a entamé un processus d’élaboration des plans de gestion des zones humides du littoral. Il est à noter que les principales activités de menaces sont entre autres la pollution de tout genre.");
    d.setLicense(License.CC_BY_NC_4_0);
    service.create(d);

    final Organization plazi = new Organization();
    plazi.setTitle("Plazi.org taxonomic treatments database");
    plazi.setCountry(Country.SWITZERLAND);
    plazi.setEndorsingNodeKey(nodeKey);
    plazi.setEndorsementApproved(true);
    plazi.setLanguage(Language.ENGLISH);
    plazi.setPassword("passw0rd");
    plazi.setKey(organizationService.create(plazi));

    d.setKey(null);
    d.setTitle("A new species of Endecous Saussure, 1878 (Orthoptera, Gryllidae) from northeast Brazil with the first X X 0 chromosomal sex system in Gryllidae");
    d.setPublishingOrganizationKey(plazi.getKey());
    d.setDescription("This dataset contains the digitized treatments in Plazi based on the original journal article Zefa, Edison, Redü, Darlan Rutz, Costa, Maria Kátia Matiotti Da, Fontanetti, Carmem S., Gottschalk, Marco Silva, Padilha, Giovanna Boff, Fernandes, Anelise, Martins, Luciano De P. (2014): A new species of Endecous Saussure, 1878 (Orthoptera, Gryllidae) from northeast Brazil with the first X X 0 chromosomal sex system in Gryllidae. Zootaxa 3847 (1): 125-132, DOI: http://dx.doi.org/10.11646/zootaxa.3847.1.7");
    d.setLicense(License.CC0_1_0);
    service.create(d);

    //Give some time to Solr to update
    Thread.sleep(100);

    assertAll(6l);
    assertSearch("Hund", 1);
    assertSearch("bli bla blub", 2);
    assertSearch("PonTaurus", 1);
    assertSearch("Pontaurus needs more than 255 characters", 1);
    assertSearch("very, very long title", 1);
    assertSearch("Bird tracking", 1);
    assertSearch("Plazi", 1);
    assertSearch("plazi.org", 1);
    assertSearch("Kátia", 1);
    assertSearch("10.11646/zootaxa.3847.1.7", 1);
    List<DatasetSearchResult> docs = assertSearch("GPS", 2);
    assertEquals(gpsKey, docs.get(0).getKey());
  }

  private void update(Organization publisher) throws InterruptedException {
    organizationService.update(publisher);
    // jenkins sometimes fails to update solr in time for the query to include the modified index.
    // allow for some extra time (it should have no bad impact on the real time index)

    // TODO: https://github.com/gbif/registry/issues/22
    Thread.sleep(100);
  }

  @Test
  @Ignore("See https://github.com/gbif/registry/issues/22")
  public void testSearchListener() throws InterruptedException {
    Dataset d = newEntity();
    d = create(d, 1);

    assertAll(1l);
    assertSearch("Pontaurus needs more than 255 characters", 1); // 1 result expected
    assertSearch("very, very long title", 1); // 1 result expected

    // update
    d.setTitle("NEW-DATASET-TITLE");
    service.update(d);
    assertAll(1l);
    assertSearch("Pontaurus", 0);
    assertSearch(d.getTitle(), 1);

    // update publishing organization title should be captured
    Organization publisher = organizationService.get(d.getPublishingOrganizationKey());
    assertSearch(publisher.getTitle(), 1);
    publisher.setTitle("OWNERTITLE");
    update(publisher);
    assertSearch(publisher.getTitle(), 1);

    publisher.setTitle("BGBM");
    update(publisher);
    assertSearch(publisher.getTitle(), 1);
    assertSearch("OWNERTITLE", 0);

    // update hosting organization title should be captured
    Installation installation = installationService.get(d.getInstallationKey());
    assertNotNull("Installation should be present", installation);
    Organization host = organizationService.get(installation.getOrganizationKey());
    assertSearch(host.getTitle(), 1);
    host.setTitle("HOSTTITLE");
    update(host);
    assertSearch(host.getTitle(), 1);

    host.setTitle("BGBM");
    update(host);
    assertSearch(host.getTitle(), 1);
    assertSearch("HOSTTITLE", 0);

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
    o.setTitle("ANEWORG");
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
  private List<DatasetSearchResult> assertSearch(String query, int expected) {
    DatasetSearchUpdateUtils.awaitUpdates(datasetIndexService); // SOLR updates are asynchronous
    DatasetSearchRequest req = new DatasetSearchRequest();
    req.setQ(query);
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    assertEquals("SOLR does not have the expected number of results for query[" + query + "]", Long.valueOf(expected), resp.getCount());
    return resp.getResults();
  }

  /**
   * Utility to verify that after waiting for SOLR to update, the given query returns the expected count of results.
   */
  private void testSearch(String query) {
    System.out.println("\n*****\n"+query);
    DatasetSearchUpdateUtils.awaitUpdates(datasetIndexService); // SOLR updates are asynchronous
    DatasetSearchRequest req = new DatasetSearchRequest();
    req.setQ(query);

    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
  }

  private void assertAll(Long expected) {
    DatasetSearchUpdateUtils.awaitUpdates(datasetIndexService); // SOLR updates are asynchronous
    DatasetSearchRequest req = new DatasetSearchRequest();
    req.setQ("*");
    SearchResponse<DatasetSearchResult, DatasetSearchParameter> resp = searchService.search(req);
    assertNotNull(resp.getCount());
    System.out.println(resp.getCount());
    System.out.println(resp);
    assertEquals("SOLR docs not as expected", expected, resp.getCount());
  }

  /**
   * Utility to verify that after waiting for SOLR to update, the given query returns the expected count of results.
   */
  private void assertSearch(Country publishingCountry, Country country, int expected) {
    DatasetSearchUpdateUtils.awaitUpdates(datasetIndexService); // SOLR updates are asynchronous
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
                 "] and publishingCountry[" + publishingCountry + "]", Long.valueOf(expected), resp.getCount());
  }

  private Dataset newEntity(@Nullable Country publisherCountry) {
    // endorsing node for the organization
    UUID nodeKey = nodeService.create(Nodes.newInstance());
    // publishing organization (required field)
    Organization o = Organizations.newInstance(nodeKey);
    if (publisherCountry != null) {
      o.setCountry(publisherCountry);
    }
    UUID organizationKey = organizationService.create(o);

    Installation i = Installations.newInstance(organizationKey);
    UUID installationKey = installationService.create(i);

    return newEntity(organizationKey, installationKey);
  }

  @Override
  protected Dataset newEntity() {
    return newEntity(null);
  }

  private Dataset newEntity(UUID organizationKey, UUID installationKey) {
    // the dataset
    Dataset d = Datasets.newInstance(organizationKey, installationKey);
    return d;
  }

  @Test
  public void testDatasetHTMLSanitizer() {
    Dataset dataset = newEntity();
    dataset.setDescription("<h1 style=\"color:red\">headline</h1><br/>" +
            "<p>paragraph with <a href=\"http://www.gbif.org\">link</a> and <em>italics</em></p>" +
            "<script>//my script</script>" +
            "<iframe src=\"perdu.com\">");

    String expectedParagraph = "<h1>headline</h1><br /><p>paragraph with <a href=\"http://www.gbif.org\">link</a> and <em>italics</em></p>";

    Map<String, Object> processProperties = Datasets.buildExpectedProcessedProperties(dataset);
    processProperties.put("description", expectedParagraph);
    dataset = create(dataset, 1, processProperties);
    assertEquals(expectedParagraph, dataset.getDescription());
  }

  @Test
  public void testCitation() {
    Dataset dataset = newEntity();
    dataset = create(dataset, 1);
    dataset = service.get(dataset.getKey());
    assertNotNull("Citation should never be null", dataset.getCitation());

    assertEquals("ABC", dataset.getCitation().getIdentifier());
    //original citation not preserved, we generate one
    assertNotEquals("This is a citation text", dataset.getCitation().getText());

    // update it
    dataset.getCitation().setIdentifier("doi:123");
    dataset.getCitation().setText("GOD publishing, volume 123");
    service.update(dataset);
    dataset = service.get(dataset.getKey());
    assertEquals("doi:123", dataset.getCitation().getIdentifier());
    //original citation not preserved, we generate one
    assertNotEquals("GOD publishing, volume 123", dataset.getCitation().getText());

    // setting to null should make it the default using the org:dataset titles
    dataset.getCitation().setText(null);
    service.update(dataset);
    dataset = service.get(dataset.getKey());
    assertEquals("doi:123", dataset.getCitation().getIdentifier());
    //original citation not preserved, we generate one
    assertNotEquals("The BGBM: Pontaurus needs more than 255 characters for it's title. It is a very, very, very, very long title in the German language. Word by word and character by character it's exact title is: \"Vegetationskundliche Untersuchungen in der Hochgebirgsregion der Bolkar Daglari & Aladaglari, Türkei\"", dataset.getCitation().getText());
  }

  @Test
  public void testDoiChanges() {
    final DOI external1 = new DOI("10.9999/nonGbif");
    final DOI external2 = new DOI("10.9999/nonGbif2");
    // we use the test prefix in tests for GBIF DOIs, see registry-test.properties
    final DOI gbif2 = new DOI("10.5072/sthelse");

    Dataset src = newEntity();
    src.setDoi(external1);
    final UUID key = create(src, 1).getKey();
    Dataset dataset = service.get(key);
    assertEquals(external1, dataset.getDoi());
    assertEquals(0, service.listIdentifiers(key).size());

    dataset.setDoi(null);
    service.update(dataset);
    dataset = service.get(key);
    assertNotNull("DOI should never be null", dataset.getDoi());
    assertFalse(dataset.getDoi().equals(external1));
    final DOI originalGBIF = dataset.getDoi();
    assertThat(service.listIdentifiers(key))
      .hasSize(1)
      .extracting("identifier").contains(external1.toString());

    dataset.setDoi(external1);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(external1, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
      .hasSize(1)
      .extracting("identifier").contains(originalGBIF.toString());

    dataset.setDoi(external2);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(external2, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
      .hasSize(2)
      .extracting("identifier").contains(originalGBIF.toString(), external1.toString());

    dataset.setDoi(null);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(originalGBIF, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
      .hasSize(2)
      .extracting("identifier").contains(external1.toString(), external2.toString());

    dataset.setDoi(gbif2);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(gbif2, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
      .hasSize(3)
      .extracting("identifier").contains(external1.toString(), external2.toString(), originalGBIF.toString());

    dataset.setDoi(external1);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(external1, dataset.getDoi());
    assertThat(service.listIdentifiers(key))
      .hasSize(3)
      .extracting("identifier").contains(gbif2.toString(), external2.toString(), originalGBIF.toString());

  }

  @Test
  public void testLicenseChanges() {
    Dataset src = newEntity();

    // start with dataset with null license
    src.setLicense(null);

    // register dataset
    final UUID key = create(src, 1).getKey();

    // ensure default license CC-BY 4.0 was assigned
    Dataset dataset = service.get(key);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());

    // try updating dataset, setting license to NULL - ensure original license was preserved
    dataset.setLicense(null);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC_BY_4_0, dataset.getLicense());

    // try updating dataset with different, less restrictive license CC0 1.0 - ensure license was replaced
    dataset.setLicense(License.CC0_1_0);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());

    // try updating dataset with an UNSUPPORTED license - ensure original license CC0 1.0 was preserved
    dataset.setLicense(License.UNSUPPORTED);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());

    // try updating dataset with an UNSPECIFIED license - ensure original license CC0 1.0 was preserved
    dataset.setLicense(License.UNSPECIFIED);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(License.CC0_1_0, dataset.getLicense());
  }

  /**
   * Test calls DatasetResource.updateFromPreferredMetadata directly, to ensure it updates the dataset by
   * reinterpreting its preferred metadata document. In particular the test ensures the dataset license is updated
   * properly as per the metadata document.
   */
  @Test
  public void testUpdateFromPreferredMetadata() throws IOException {
      Dataset src = newEntity();

      // start with dataset with CC0 license
      src.setLicense(License.CC0_1_0);

      // register dataset
      final UUID key = create(src, 1).getKey();

      // ensure license CC0 was assigned
      Dataset dataset = service.get(key);
      assertEquals(License.CC0_1_0, dataset.getLicense());

      // insert metadata document, which sets license to more restrictive license CC-BY - ensure license was replaced
      InputStream is = Resources.getResourceAsStream("metadata/sample-v1.1.xml");
      service.insertMetadata(key, is);
      dataset = service.get(key);
      assertEquals(License.CC_BY_4_0, dataset.getLicense());

      // update dataset, overwritting license back to CC0
      dataset.setLicense(License.CC0_1_0);
      service.update(dataset);
      dataset = service.get(key);
      assertEquals(License.CC0_1_0, dataset.getLicense());

      // last, update dataset from preferred metadata document, ensuring license gets reset to CC-BY
      DatasetResource datasetResource = webservice().getBinding(DatasetResource.class).getProvider().get();
      datasetResource.updateFromPreferredMetadata(key, "DatasetIT");
      dataset = service.get(key);
      assertEquals(License.CC_BY_4_0, dataset.getLicense());
  }

  /**
   * Test checks behaviour updating Citation with valid and invalid identifier.
   * In the database, there is a min length 1 character constraint on Dataset.citation_identifier.
   */
  @Test
  public void testDatasetCitationIdentifierConstraint() throws IOException {
    Dataset src = newEntity();

    // register dataset
    final UUID key = create(src, 1).getKey();

    Dataset dataset = service.get(key);
    assertNotNull(dataset.getCitation());

    // set Citation identifier to null
    Citation c = dataset.getCitation();
    c.setIdentifier(null);
    dataset.setCitation(c);
    service.update(dataset);

    // check update succeeds
    dataset = service.get(key);
    assertNotNull(dataset.getCitation());
    // we use the generated citation
    assertNotEquals("This is a citation text", dataset.getCitation().getText());
    assertNull(dataset.getCitation().getIdentifier());

    // set Citation identifier to single character
    c = dataset.getCitation();
    c.setIdentifier("");
    dataset.setCitation(c);

    // update dataset...
    ConstraintViolationException exception = null;
    try {
      service.update(dataset);
    } catch (ConstraintViolationException e) {
      exception = e;
    }
    // /...and check it fails, however, constraint violation can only be thrown by web service because client
    // trims Citation fields via StringTrimInterceptor
    if (service.equals(webservice().getBinding(DatasetService.class).getProvider().get())) {
      assertNotNull(exception);
    }
  }

  @Test
  public void testMaintenanceUpdateFrequencyChanges() {
    Dataset src = newEntity();
    assertNull(src.getMaintenanceUpdateFrequency());
    final UUID key = create(src, 1).getKey();
    Dataset dataset = service.get(key);
    assertNull(src.getMaintenanceUpdateFrequency());

    // try updating maintenanceUpdateFrequency - ensure value persisted
    dataset.setMaintenanceUpdateFrequency(MaintenanceUpdateFrequency.AS_NEEDED);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(MaintenanceUpdateFrequency.AS_NEEDED, dataset.getMaintenanceUpdateFrequency());

    // try updating maintenanceUpdateFrequency again - ensure value replaced
    dataset.setMaintenanceUpdateFrequency(MaintenanceUpdateFrequency.BIANNUALLY);
    service.update(dataset);
    dataset = service.get(key);
    assertEquals(MaintenanceUpdateFrequency.BIANNUALLY, dataset.getMaintenanceUpdateFrequency());
  }

  @Test
  public void test404() throws IOException {
    assertNull(service.get(UUID.randomUUID()));
  }

  @Test
  public void testMetadata() throws IOException {
    Dataset d1 = create(newEntity(), 1);
    assertEquals(License.CC_BY_NC_4_0, d1.getLicense());
    List<Metadata> metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals("No EML uploaded yes", 0, metadata.size());

    // upload a valid EML document (without a machine readable license)
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));

    // verify dataset was updated from parsed document
    Dataset d2 = service.get(d1.getKey());
    assertNotEquals("Dataset should have changed after metadata was uploaded", d1, d2);
    assertEquals("Tanzanian Entomological Collection", d2.getTitle());
    assertEquals("Created data should not change", d1.getCreated(), d2.getCreated());
    assertTrue("Dataset modification date should change", d1.getModified().before(d2.getModified()));

    // verify license stayed the same, because no machine readable license was detected in EML document
    assertEquals(License.CC_BY_NC_4_0, d2.getLicense());

    // verify EML document was stored successfully
    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals("Exactly one EML uploaded", 1, metadata.size());
    assertEquals("Wrong metadata type", MetadataType.EML, metadata.get(0).getType());

    // check number of stored DC documents
    metadata = service.listMetadata(d1.getKey(), MetadataType.DC);
    assertTrue("No Dublin Core uplaoded yet", metadata.isEmpty());

    // upload subsequent DC document which has less priority than the previous EML document
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/worms_dc.xml"));

    // verify dataset has NOT changed
    Dataset d3 = service.get(d1.getKey());
    assertEquals("Tanzanian Entomological Collection", d3.getTitle());
    assertEquals("Created data should not change", d1.getCreated(), d3.getCreated());

    // verify DC document was stored successfully
    metadata = service.listMetadata(d1.getKey(), MetadataType.DC);
    assertEquals("Exactly one DC uploaded", 1, metadata.size());
    assertEquals("Wrong metadata type", MetadataType.DC, metadata.get(0).getType());

    // upload 2nd EML doc (with a machine readable license), which has higher priority than the previous EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample-v1.1.xml"));

    // verify dataset was updated from parsed document
    Dataset d4 = service.get(d1.getKey());
    assertEquals("Sample Metadata RLS", d4.getTitle());

    // verify license was updated because CC-BY 4.0 license was detected in EML document
    assertEquals(License.CC_BY_4_0, d4.getLicense());

    // verify EML document replaced EML docuemnt of less priority
    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals("Exactly one EML uploaded", 1, metadata.size());

    // upload 3rd EML doc (with a machine readable UNSUPPORTED license), which has higher priority than the previous EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample-v1.1-unsupported-license.xml"));

    // verify dataset was updated from parsed document
    Dataset d5 = service.get(d1.getKey());
    assertEquals("Sample Metadata RLS (2)", d5.getTitle());

    // verify license was NOT updated because UNSUPPORTED license was detected in EML document (only supported license
    // can overwrite existing license)
    assertEquals(License.CC_BY_4_0, d5.getLicense());

    // verify EML document replaced EML document of less priority
    metadata = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals("Exactly one EML uploaded", 1, metadata.size());
    int lastEmlMetadataDocKey = metadata.get(0).getKey();

    // upload exact same EML doc again, but make sure it does NOT update dataset!
    Dataset lastUpated = service.get(d1.getKey());
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample-v1.1-unsupported-license.xml"));

    // verify dataset was NOT updated from parsed document
    Dataset d6 = service.get(d1.getKey());
    assertTrue(d6.getModified().compareTo(lastUpated.getModified()) == 0);

    // verify EML document NOT replaced
    List<Metadata> metadata2 = service.listMetadata(d1.getKey(), MetadataType.EML);
    int emlMetadataDocKey = metadata2.get(0).getKey();
    assertEquals(lastEmlMetadataDocKey, emlMetadataDocKey);

    // verify original EML document can be retrieved by WS request (verify POR-3170 fixed)
    InputStream persistedDocument = service.getMetadataDocument(emlMetadataDocKey);
    String persistedDocumentContent = CharStreams.toString(new InputStreamReader(persistedDocument, Charsets.UTF_8));
    InputStream originalDocument = FileUtils.classpathStream("metadata/sample-v1.1-unsupported-license.xml");
    String originalDocumentContent = CharStreams.toString(new InputStreamReader(originalDocument, Charsets.UTF_8));
    assertEquals(originalDocumentContent, persistedDocumentContent);
  }

  /**
   * Test that uploading the same document repeatedly does not change the dataset.
   */
  @Test
  public void testMetadataDuplication() throws IOException {
    Dataset d1 = newAndCreate(1);
    List<Metadata> m1 = service.listMetadata(d1.getKey(), MetadataType.EML);

    // upload a valid EML doc
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));

    // verify our dataset has changed
    Dataset d2 = service.get(d1.getKey());
    assertNotEquals("Dataset should have changed after metadata was uploaded", d1, d2);
    List<Metadata> m2 = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertNotEquals("Dataset metadata should have changed after metadata was uploaded", m1, m2);

    // upload the doc a second time - it should not update the metadata
    service.insertMetadata(d1.getKey(), FileUtils.classpathStream("metadata/sample.xml"));
    List<Metadata> m3 = service.listMetadata(d1.getKey(), MetadataType.EML);
    assertEquals("Dataset metadata should not have changed after same metadata document was uploaded", m2, m3);
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
    createDatasetsWithType(DatasetType.SAMPLING_EVENT, 4);

    assertResultsOfSize(service.listByType(DatasetType.METADATA, new PagingRequest()), 1);
    assertResultsOfSize(service.listByType(DatasetType.CHECKLIST, new PagingRequest()), 2);
    assertResultsOfSize(service.listByType(DatasetType.OCCURRENCE, new PagingRequest()), 3);
    assertResultsOfSize(service.listByType(DatasetType.SAMPLING_EVENT, new PagingRequest()), 4);
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

  /**
   * Create a new instance of Dataset, store it using the create method.
   * @param expectedCount
   * @return
   */
  private Dataset newAndCreate(int expectedCount) {
    Dataset newDataset = newEntity();
    return create(newDataset, expectedCount);
  }


}
