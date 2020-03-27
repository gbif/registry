package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.DatabaseInitializer;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;

import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests the {@link org.gbif.registry.ws.resources.collections.IdentifierResolverResource}. */
@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class IdentifierResolverIT {

  private static final String BASE_URL = "/" + GRSCICOLL_PATH + "/resolve";
  private static final String IDENTIFIER_PARAM = "identifier";
  private static final String TEST_USER = "test";

  private static final String IDENTIFIER1 = "http://grbio.org/cool/g9da-xpan";
  private static final String IDENTIFIER2 = "urn:lsid:biocol.org:col:35158";
  private static final String IDENTIFIER3 = "http://grscicoll.org/cool/kx98-stkb";
  private static final String IDENTIFIER4 = "http://usfsc.grscicoll.org/cool/i6ah-3d5y";

  @ClassRule public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

  @Autowired private MockMvc mockMvc;
  @Autowired private CollectionService collectionService;
  @Autowired private InstitutionService institutionService;

  @Test
  public void findCollectionByIdentifierTest() throws Exception {
    // State
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("col1");
    collection.setCreatedBy(TEST_USER);
    collection.setModifiedBy(TEST_USER);
    UUID collectionKey = collectionService.create(collection);

    // add identifier to collection
    Identifier id1 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER1);
    id1.setCreatedBy(TEST_USER);
    collectionService.addIdentifier(collectionKey, id1);

    // there could be duplicates since we don't check it
    Identifier id2 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER1);
    id2.setCreatedBy(TEST_USER);
    collectionService.addIdentifier(collectionKey, id2);

    // find collection by cool URI and env
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER1.replace("http://", "dev.")))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/collection/" + collectionKey)));

    // find collection by cool URI without env
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER1))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/collection/" + collectionKey)));
  }

  @Test
  public void findInstitutionByIdentifiersTest() throws Exception {
    // State
    Institution institution = new Institution();
    institution.setCode("i1");
    institution.setName("inst1");
    institution.setCreatedBy(TEST_USER);
    institution.setModifiedBy(TEST_USER);
    UUID institutionKey = institutionService.create(institution);

    // add identifiers to institution
    Identifier id3 = new Identifier(IdentifierType.LSID, IDENTIFIER2);
    id3.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(institutionKey, id3);

    Identifier id4 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER3);
    id4.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(institutionKey, id4);

    Identifier id5 = new Identifier(IdentifierType.GRSCICOLL_URI, IDENTIFIER4);
    id5.setCreatedBy(TEST_USER);
    institutionService.addIdentifier(institutionKey, id5);

    // find institution by LSID
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER2))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/institution/" + institutionKey)));

    // find institution by GrSciColl URI and env
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER3.replace("http://", "dev.")))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/institution/" + institutionKey)));

    // find institution by USFSC URI
    mockMvc
        .perform(get(BASE_URL).queryParam(IDENTIFIER_PARAM, IDENTIFIER4.replace("http://", "")))
        .andExpect(status().isSeeOther())
        .andExpect(header().string("Location", endsWith("/institution/" + institutionKey)));
  }

  @Test
  public void unknownIdentifier() throws Exception {
    mockMvc.perform(get(BASE_URL + "dev.grbio.org/cool/foo")).andExpect(status().isNotFound());
  }
}
