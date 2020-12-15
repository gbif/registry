/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.it.collections;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.lookup.CollectionMatched;
import org.gbif.api.model.collections.lookup.InstitutionMatched;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.service.collections.lookup.LookupService;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link LookupService}. */
public class LookupServiceIT extends BaseItTest {

  private final Institution i1 = new Institution();
  private final Institution i2 = new Institution();
  private final Collection c1 = new Collection();
  private final Collection c2 = new Collection();

  private final LookupService lookupService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;
  private final DatasetService datasetService;
  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final InstallationService installationService;

  @Autowired
  public LookupServiceIT(
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer,
      LookupService lookupService,
      InstitutionService institutionService,
      CollectionService collectionService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService) {
    super(simplePrincipalProvider, esServer);
    this.lookupService = lookupService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
    this.datasetService = datasetService;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.installationService = installationService;
  }

  @BeforeEach
  public void loadData() {
    i1.setCode("I1");
    i1.setName("Institution 1");
    Address address1 = new Address();
    address1.setCountry(Country.AFGHANISTAN);
    i1.setAddress(address1);
    institutionService.create(i1);

    i2.setCode("I2");
    i2.setName("Institution 2");
    i2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("II2", "test")));
    i2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-inst"));
    institutionService.create(i2);

    c1.setCode("C1");
    c1.setName("Collection 1");
    c1.setInstitutionKey(i1.getKey());
    collectionService.create(c1);

    c2.setCode("C2");
    c2.setName("Collection 2");
    c2.setInstitutionKey(i2.getKey());
    c2.setAlternativeCodes(Collections.singletonList(new AlternativeCode("CC2", "test")));
    c2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-coll"));
    collectionService.create(c2);
  }

  @Test
  public void lookupByCodeTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i1.getCode());
    params.setCollectionCode(c1.getCode());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(1, institutionMatch.getReasons().size());
    assertEquals(Match.Reason.CODE_MATCH, institutionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.FUZZY, collectionMatch.getMatchType());
    assertEquals(c1.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(1, collectionMatch.getReasons().size());
    assertEquals(Match.Reason.CODE_MATCH, collectionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, collectionMatch.getStatus());
  }

  @Test
  public void lookupByNameTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i1.getName().toUpperCase() + "  ");
    params.setCollectionCode(c1.getName().toUpperCase() + "  ");

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(1, institutionMatch.getReasons().size());
    assertEquals(Match.Reason.NAME_MATCH, institutionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.FUZZY, collectionMatch.getMatchType());
    assertEquals(c1.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(1, collectionMatch.getReasons().size());
    assertEquals(Match.Reason.NAME_MATCH, collectionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, collectionMatch.getStatus());
  }

  @Test
  public void lookupByIdentifierTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());
    params.setCollectionId("urn:uuid:" + c2.getIdentifiers().get(0).getIdentifier());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i2.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(1, institutionMatch.getReasons().size());
    assertEquals(Match.Reason.IDENTIFIER_MATCH, institutionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.FUZZY, collectionMatch.getMatchType());
    assertEquals(c2.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(1, collectionMatch.getReasons().size());
    assertEquals(Match.Reason.IDENTIFIER_MATCH, collectionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, collectionMatch.getStatus());
  }

  @Test
  public void lookupByAlternativeCodeTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i2.getAlternativeCodes().get(0).getCode());
    params.setCollectionCode(c2.getAlternativeCodes().get(0).getCode());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i2.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(1, institutionMatch.getReasons().size());
    assertEquals(
        Match.Reason.ALTERNATIVE_CODE_MATCH, institutionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.FUZZY, collectionMatch.getMatchType());
    assertEquals(c2.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(1, collectionMatch.getReasons().size());
    assertEquals(
        Match.Reason.ALTERNATIVE_CODE_MATCH, collectionMatch.getReasons().iterator().next());
    assertEquals(Match.Status.DOUBTFUL, collectionMatch.getStatus());
  }

  @Test
  public void lookupByCodeAndIdTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i2.getCode());
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());
    params.setCollectionCode(c2.getCode());
    params.setCollectionId(c2.getIdentifiers().get(0).getIdentifier());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.EXACT, institutionMatch.getMatchType());
    assertEquals(i2.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(2, institutionMatch.getReasons().size());
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.IDENTIFIER_MATCH));
    assertEquals(Match.Status.ACCEPTED, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.EXACT, collectionMatch.getMatchType());
    assertEquals(c2.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(2, collectionMatch.getReasons().size());
    assertTrue(collectionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertTrue(collectionMatch.getReasons().contains(Match.Reason.IDENTIFIER_MATCH));
    assertEquals(Match.Status.ACCEPTED, collectionMatch.getStatus());
  }

  @Test
  public void ownerInstitutionCodeTest() {
    // State
    LookupParams params = new LookupParams();
    params.setOwnerInstitutionCode("foo");
    params.setInstitutionCode(i2.getCode());
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());
    params.setVerbose(true);

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertEquals(Match.MatchType.NONE, result.getInstitutionMatch().getMatchType());
    assertEquals(Match.Status.AMBIGUOUS_OWNER, result.getInstitutionMatch().getStatus());
    assertEquals(Match.MatchType.NONE, result.getCollectionMatch().getMatchType());
    assertNull(result.getCollectionMatch().getStatus());
    assertEquals(1, result.getAlternativeMatches().getInstitutionMatches().size());

    Match<InstitutionMatched> alternative =
        result.getAlternativeMatches().getInstitutionMatches().get(0);
    assertEquals(Match.MatchType.EXACT, alternative.getMatchType());
    assertEquals(i2.getKey(), alternative.getEntityMatched().getKey());
    assertEquals(3, alternative.getReasons().size());
    assertTrue(alternative.getReasons().contains(Match.Reason.CODE_MATCH));
    assertTrue(alternative.getReasons().contains(Match.Reason.IDENTIFIER_MATCH));
    assertTrue(alternative.getReasons().contains(Match.Reason.DIFFERENT_OWNER));
  }

  @Test
  public void institutionCollectionMismatchTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i1.getCode());
    params.setCollectionCode(c2.getCode());
    params.setCollectionId(c2.getIdentifiers().get(0).getIdentifier());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(1, institutionMatch.getReasons().size());
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.EXACT, collectionMatch.getMatchType());
    assertEquals(c2.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(3, collectionMatch.getReasons().size());
    assertTrue(collectionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertTrue(collectionMatch.getReasons().contains(Match.Reason.IDENTIFIER_MATCH));
    assertTrue(collectionMatch.getReasons().contains(Match.Reason.INST_COLL_MISMATCH));
    assertEquals(Match.Status.ACCEPTED, collectionMatch.getStatus());
  }

  @Test
  public void institutionCollectionMismatchAmbiguousTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i1.getCode());
    params.setCollectionId(c2.getIdentifiers().get(0).getIdentifier());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(1, institutionMatch.getReasons().size());
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertEquals(Match.MatchType.NONE, result.getCollectionMatch().getMatchType());
    assertEquals(
        Match.Status.AMBIGUOUS_INSTITUTION_MISMATCH, result.getCollectionMatch().getStatus());
  }

  @Test
  public void explicitMappingsTest() {
    // State
    Dataset d1 = createDataset();

    OccurrenceMapping occMappingI1 = new OccurrenceMapping();
    occMappingI1.setDatasetKey(d1.getKey());
    institutionService.addOccurrenceMapping(i1.getKey(), occMappingI1);

    OccurrenceMapping occMappingC1 = new OccurrenceMapping();
    occMappingC1.setDatasetKey(d1.getKey());
    occMappingC1.setCode(c1.getCode());
    collectionService.addOccurrenceMapping(c1.getKey(), occMappingC1);

    LookupParams params = new LookupParams();
    params.setDatasetKey(d1.getKey());
    params.setInstitutionCode(i1.getCode());
    params.setCollectionCode(c1.getCode());
    params.setDatasetKey(d1.getKey());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.EXPLICIT_MAPPING, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(0, institutionMatch.getReasons().size());
    assertEquals(Match.Status.ACCEPTED, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.EXPLICIT_MAPPING, collectionMatch.getMatchType());
    assertEquals(c1.getKey(), collectionMatch.getEntityMatched().getKey());
    assertEquals(0, collectionMatch.getReasons().size());
    assertEquals(Match.Status.ACCEPTED, collectionMatch.getStatus());
  }

  @Test
  public void ignoredExplicitMappingsTest() {
    // State
    Dataset d1 = createDataset();

    OccurrenceMapping occMappingI1 = new OccurrenceMapping();
    occMappingI1.setDatasetKey(d1.getKey());
    occMappingI1.setCode("foo");
    institutionService.addOccurrenceMapping(i1.getKey(), occMappingI1);

    OccurrenceMapping occMappingC1 = new OccurrenceMapping();
    occMappingC1.setDatasetKey(d1.getKey());
    occMappingC1.setCode("bar");
    collectionService.addOccurrenceMapping(c1.getKey(), occMappingC1);

    LookupParams params = new LookupParams();
    params.setDatasetKey(d1.getKey());
    params.setInstitutionCode(i1.getCode());
    params.setCollectionCode(c1.getCode());
    params.setDatasetKey(d1.getKey());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.FUZZY, collectionMatch.getMatchType());
  }

  @Test
  public void ambiguousExplicitMappingsTest() {
    // State
    Dataset d1 = createDataset();

    OccurrenceMapping occMappingI2 = new OccurrenceMapping();
    occMappingI2.setDatasetKey(d1.getKey());
    occMappingI2.setCode("foo");
    institutionService.addOccurrenceMapping(i1.getKey(), occMappingI2);

    OccurrenceMapping occMappingI22 = new OccurrenceMapping();
    occMappingI22.setDatasetKey(d1.getKey());
    occMappingI22.setCode("foo2");
    institutionService.addOccurrenceMapping(i2.getKey(), occMappingI22);

    OccurrenceMapping occMappingC2 = new OccurrenceMapping();
    occMappingC2.setDatasetKey(d1.getKey());
    occMappingC2.setCode("code_test)");
    collectionService.addOccurrenceMapping(c1.getKey(), occMappingC2);

    OccurrenceMapping occMappingC22 = new OccurrenceMapping();
    occMappingC22.setDatasetKey(d1.getKey());
    occMappingC22.setIdentifier("id_test");
    collectionService.addOccurrenceMapping(c2.getKey(), occMappingC22);

    LookupParams params = new LookupParams();
    params.setDatasetKey(d1.getKey());
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());
    params.setCollectionCode(occMappingC2.getCode());
    params.setCollectionId(occMappingC22.getIdentifier());
    params.setDatasetKey(d1.getKey());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.NONE, institutionMatch.getMatchType());
    assertEquals(Match.Status.AMBIGUOUS_EXPLICIT_MAPPINGS, institutionMatch.getStatus());

    assertNotNull(result.getCollectionMatch());
    Match<CollectionMatched> collectionMatch = result.getCollectionMatch();
    assertEquals(Match.MatchType.NONE, collectionMatch.getMatchType());
    assertEquals(Match.Status.AMBIGUOUS_EXPLICIT_MAPPINGS, collectionMatch.getStatus());
  }

  @Test
  public void countryMatchTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i1.getCode());
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());
    params.setCountry(Country.AFGHANISTAN);
    params.setVerbose(true);

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(2, institutionMatch.getReasons().size());
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.COUNTRY_MATCH));
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());

    assertEquals(1, result.getAlternativeMatches().getInstitutionMatches().size());
    Match<InstitutionMatched> alternative =
        result.getAlternativeMatches().getInstitutionMatches().get(0);
    assertEquals(Match.MatchType.FUZZY, alternative.getMatchType());
    assertEquals(i2.getKey(), alternative.getEntityMatched().getKey());
    assertEquals(1, alternative.getReasons().size());
    assertTrue(alternative.getReasons().contains(Match.Reason.IDENTIFIER_MATCH));
    assertNull(alternative.getStatus());
  }

  @Test
  public void keyMatchTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i1.getCode());
    params.setInstitutionId(i1.getKey().toString());
    params.setVerbose(true);

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertNotNull(result.getInstitutionMatch());
    Match<InstitutionMatched> institutionMatch = result.getInstitutionMatch();
    assertEquals(Match.MatchType.FUZZY, institutionMatch.getMatchType());
    assertEquals(i1.getKey(), institutionMatch.getEntityMatched().getKey());
    assertEquals(2, institutionMatch.getReasons().size());
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.KEY_MATCH));
    assertTrue(institutionMatch.getReasons().contains(Match.Reason.CODE_MATCH));
    assertEquals(Match.Status.DOUBTFUL, institutionMatch.getStatus());
  }

  private Dataset createDataset() {
    Node node = new Node();
    node.setTitle("node");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    nodeService.create(node);

    Organization org = new Organization();
    org.setEndorsingNodeKey(node.getKey());
    org.setTitle("organization");
    org.setLanguage(Language.ABKHAZIAN);
    org.setPassword("testtttt");
    organizationService.create(org);

    Installation installation = new Installation();
    installation.setTitle("title");
    installation.setOrganizationKey(org.getKey());
    installation.setType(InstallationType.BIOCASE_INSTALLATION);
    installationService.create(installation);

    Dataset dataset = new Dataset();
    dataset.setTitle("title");
    dataset.setInstallationKey(installation.getKey());
    dataset.setPublishingOrganizationKey(org.getKey());
    dataset.setType(DatasetType.CHECKLIST);
    dataset.setLanguage(Language.ABKHAZIAN);
    dataset.setLicense(License.CC0_1_0);
    datasetService.create(dataset);

    return dataset;
  }
}
