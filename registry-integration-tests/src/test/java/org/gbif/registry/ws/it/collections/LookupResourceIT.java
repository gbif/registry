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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.lookup.LookupParams;
import org.gbif.api.model.collections.lookup.LookupResult;
import org.gbif.api.model.collections.lookup.Match;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.service.collections.LookupService;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.service.collections.LookupServiceImpl.COLLECTION_TAG_NAME;
import static org.gbif.registry.service.collections.LookupServiceImpl.COLLECTION_TO_INSTITUTION_TAG_NAME;
import static org.gbif.registry.service.collections.LookupServiceImpl.INSTITUTION_TAG_NAME;
import static org.gbif.registry.service.collections.LookupServiceImpl.INSTITUTION_TO_COLLECTION_TAG_NAME;
import static org.gbif.registry.service.collections.LookupServiceImpl.PROCESSING_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link org.gbif.registry.ws.resources.collections.LookupResource}. */
public class LookupResourceIT extends BaseItTest {

  private static final String INST_TAG_CODE = "inst";
  private static final String COLL_TAG_CODE = "coll";
  private static final String INST_TO_COLL_TAG_CODE = "inst2col";
  private static final String COLL_TO_INST_TAG_CODE = "col2inst";

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
  public LookupResourceIT(
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
    institutionService.create(i1);

    i2.setCode("I2");
    i2.setName("Institution 2");
    i2.setAlternativeCodes(Collections.singletonMap("II2", "test"));
    i2.getIdentifiers().add(new Identifier(IdentifierType.LSID, "lsid-inst"));
    institutionService.create(i2);

    c1.setCode("C1");
    c1.setName("Collection 1");
    c1.setInstitutionKey(i1.getKey());
    collectionService.create(c1);

    c2.setCode("C2");
    c2.setName("Collection 2");
    c2.setInstitutionKey(i2.getKey());
    c2.setAlternativeCodes(Collections.singletonMap("CC2", "test"));
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
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchInst.getType());
    assertEquals(i1.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertEquals(Match.MatchRemark.CODE_MATCH, matchInst.getRemarks().iterator().next());

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchColl.getType());
    assertEquals(c1.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(1, matchColl.getRemarks().size());
    assertEquals(Match.MatchRemark.CODE_MATCH, matchColl.getRemarks().iterator().next());
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
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchInst.getType());
    assertEquals(i1.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertEquals(Match.MatchRemark.NAME_MATCH, matchInst.getRemarks().iterator().next());

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchColl.getType());
    assertEquals(c1.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(1, matchColl.getRemarks().size());
    assertEquals(Match.MatchRemark.NAME_MATCH, matchColl.getRemarks().iterator().next());
  }

  @Test
  public void lookupByIdentifierTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());
    params.setCollectionId(c2.getIdentifiers().get(0).getIdentifier());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchInst.getType());
    assertEquals(i2.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertEquals(Match.MatchRemark.IDENTIFIER_MATCH, matchInst.getRemarks().iterator().next());

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchColl.getType());
    assertEquals(c2.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(1, matchColl.getRemarks().size());
    assertEquals(Match.MatchRemark.IDENTIFIER_MATCH, matchColl.getRemarks().iterator().next());
  }

  @Test
  public void lookupByAlternativeCodeTest() {
    // State
    LookupParams params = new LookupParams();
    params.setInstitutionCode(i2.getAlternativeCodes().keySet().iterator().next());
    params.setCollectionCode(c2.getAlternativeCodes().keySet().iterator().next());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchInst.getType());
    assertEquals(i2.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertEquals(
        Match.MatchRemark.ALTERNATIVE_CODE_MATCH, matchInst.getRemarks().iterator().next());

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchColl.getType());
    assertEquals(c2.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(1, matchColl.getRemarks().size());
    assertEquals(
        Match.MatchRemark.ALTERNATIVE_CODE_MATCH, matchColl.getRemarks().iterator().next());
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
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.EXACT, matchInst.getType());
    assertEquals(i2.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(2, matchInst.getRemarks().size());
    assertTrue(matchInst.getRemarks().contains(Match.MatchRemark.CODE_MATCH));
    assertTrue(matchInst.getRemarks().contains(Match.MatchRemark.IDENTIFIER_MATCH));

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.EXACT, matchColl.getType());
    assertEquals(c2.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(2, matchColl.getRemarks().size());
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.CODE_MATCH));
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.IDENTIFIER_MATCH));
  }

  @Test
  public void ownerInstitutionCodeTest() {
    // State
    LookupParams params = new LookupParams();
    params.setOwnerInstitutionCode("foo");
    params.setInstitutionCode(i2.getCode());
    params.setInstitutionId(i2.getIdentifiers().get(0).getIdentifier());

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertEquals(1, result.getInstitutionMatches().size());
    assertEquals(0, result.getCollectionMatches().size());
    Match<Institution> match = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.EXACT, match.getType());
    assertEquals(i2.getKey(), match.getEntityMatched().getKey());
    assertEquals(3, match.getRemarks().size());
    assertTrue(match.getRemarks().contains(Match.MatchRemark.CODE_MATCH));
    assertTrue(match.getRemarks().contains(Match.MatchRemark.IDENTIFIER_MATCH));
    assertTrue(match.getRemarks().contains(Match.MatchRemark.PROBABLY_ON_LOAN));
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
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.FUZZY, matchInst.getType());
    assertEquals(i1.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertTrue(matchInst.getRemarks().contains(Match.MatchRemark.CODE_MATCH));

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.EXACT, matchColl.getType());
    assertEquals(c2.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(3, matchColl.getRemarks().size());
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.CODE_MATCH));
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.IDENTIFIER_MATCH));
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.INST_COLL_MISMATCH));
  }

  @Test
  public void machineTagCodesTest() {
    // State
    Dataset d1 =
        createDatasetWithMachineTags(
            new MachineTag(
                PROCESSING_NAMESPACE, INSTITUTION_TAG_NAME, i1.getKey() + ":" + INST_TAG_CODE),
            new MachineTag(
                PROCESSING_NAMESPACE, COLLECTION_TAG_NAME, c1.getKey() + ":" + COLL_TAG_CODE));
    LookupParams params = new LookupParams();
    params.setDatasetKey(d1.getKey());
    params.setInstitutionCode(INST_TAG_CODE);
    params.setCollectionCode(COLL_TAG_CODE);

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.MACHINE_TAG, matchInst.getType());
    assertEquals(i1.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertTrue(matchInst.getRemarks().contains(Match.MatchRemark.INSTITUTION_TAG));

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.MACHINE_TAG, matchColl.getType());
    assertEquals(c1.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(1, matchColl.getRemarks().size());
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.COLLECTION_TAG));
  }

  @Test
  public void machineTagsConversionTest() {
    // State
    Dataset d2 =
        createDatasetWithMachineTags(
            new MachineTag(
                PROCESSING_NAMESPACE,
                INSTITUTION_TO_COLLECTION_TAG_NAME,
                c2.getKey() + ":" + INST_TO_COLL_TAG_CODE),
            new MachineTag(
                PROCESSING_NAMESPACE,
                COLLECTION_TO_INSTITUTION_TAG_NAME,
                i2.getKey() + ":" + COLL_TO_INST_TAG_CODE));
    LookupParams params = new LookupParams();
    params.setDatasetKey(d2.getKey());
    params.setInstitutionCode(INST_TO_COLL_TAG_CODE);
    params.setCollectionCode(COLL_TO_INST_TAG_CODE);

    // When
    LookupResult result = lookupService.lookup(params);

    // Should
    assertEquals(1, result.getInstitutionMatches().size());
    Match<Institution> matchInst = result.getInstitutionMatches().iterator().next();
    assertEquals(Match.MatchType.MACHINE_TAG, matchInst.getType());
    assertEquals(i2.getKey(), matchInst.getEntityMatched().getKey());
    assertEquals(1, matchInst.getRemarks().size());
    assertTrue(matchInst.getRemarks().contains(Match.MatchRemark.COLLECTION_TO_INSTITUTION_TAG));

    assertEquals(1, result.getCollectionMatches().size());
    Match<Collection> matchColl = result.getCollectionMatches().iterator().next();
    assertEquals(Match.MatchType.MACHINE_TAG, matchColl.getType());
    assertEquals(c2.getKey(), matchColl.getEntityMatched().getKey());
    assertEquals(1, matchColl.getRemarks().size());
    assertTrue(matchColl.getRemarks().contains(Match.MatchRemark.INSTITUTION_TO_COLLECTION_TAG));
  }

  private Dataset createDatasetWithMachineTags(MachineTag... machineTags) {
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

    for (MachineTag machineTag : machineTags) {
      datasetService.addMachineTag(dataset.getKey(), machineTag);
      dataset.getMachineTags().add(machineTag);
    }

    return dataset;
  }
}
