/*
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
package org.gbif.registry.ws.it.collections.service;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.CollectionsSortField;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.SortOrder;
import org.gbif.api.vocabulary.collections.Discipline;
import org.gbif.api.vocabulary.collections.IdType;
import org.gbif.api.vocabulary.collections.InstitutionGovernance;
import org.gbif.api.vocabulary.collections.InstitutionType;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.service.collections.duplicates.InstitutionDuplicatesService;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link InstitutionService}. */
public class InstitutionServiceIT extends BaseCollectionEntityServiceIT<Institution> {

  private final InstitutionService institutionService;

  @Autowired
  public InstitutionServiceIT(
      InstitutionService institutionService,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      InstitutionDuplicatesService duplicatesService) {
    super(
        institutionService,
        datasetService,
        nodeService,
        organizationService,
        installationService,
        principalProvider,
        duplicatesService,
        Institution.class);
    this.institutionService = institutionService;
  }

  @Test
  public void listTest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("c1");
    institution1.setName("n1");
    institution1.setActive(true);
    institution1.setType(InstitutionType.HERBARIUM);
    institution1.setInstitutionalGovernance(InstitutionGovernance.ACADEMIC_FEDERAL);
    institution1.setDisciplines(Collections.singletonList(Discipline.OCEAN));
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    address.setCountry(Country.DENMARK);
    institution1.setAddress(address);
    institution1.setNumberSpecimens(100);
    institution1.setDisplayOnNHCPortal(false);
    institution1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    UUID key1 = institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("c2");
    institution2.setName("n2");
    institution2.setActive(false);
    institution2.setDisciplines(Arrays.asList(Discipline.OCEAN, Discipline.AGRICULTURAL));
    institution2.setNumberSpecimens(200);
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    address2.setCountry(Country.SPAIN);
    institution2.setAddress(address2);
    UUID key2 = institutionService.create(institution2);

    PagingResponse<Institution> response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("dummy").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("city").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().query("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    // code and name params
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().code("c1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().name("n2").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder().code("c1").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(
                InstitutionSearchRequest.builder().code("c2").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // query param
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().query("c").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().query("dum add").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(InstitutionSearchRequest.builder().query("<").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        institutionService
            .list(InstitutionSearchRequest.builder().query("\"<\"").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(InstitutionSearchRequest.builder().query("  ").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().active(true).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .type(InstitutionType.HERBARIUM)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .institutionalGovernance(InstitutionGovernance.ACADEMIC_FEDERAL)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .disciplines(Collections.singletonList(Discipline.AGRICULTURAL))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());
    assertEquals(
        2,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .disciplines(Arrays.asList(Discipline.OCEAN, Discipline.AGRICULTURAL))
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    // alternative code
    response =
        institutionService.list(
            InstitutionSearchRequest.builder().alternativeCode("alt").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().alternativeCode("foo").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .country(Collections.singletonList(Country.SPAIN))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .country(Collections.singletonList(Country.AFGHANISTAN))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .country(Arrays.asList(Country.SPAIN, Country.AFGHANISTAN))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(1, response.getResults().size());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .country(Arrays.asList(Country.SPAIN, Country.DENMARK))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .gbifRegion(Collections.singletonList(GbifRegion.EUROPE))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(2, response.getResults().size());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder()
                .country(Collections.singletonList(Country.SPAIN))
                .gbifRegion(Collections.singletonList(GbifRegion.ASIA))
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().city("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());
    response =
        institutionService.list(
            InstitutionSearchRequest.builder().city("foo").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());

    // update address
    institution2 = institutionService.get(key2);
    assertNotNull(institution2.getAddress());
    institution2.getAddress().setCity("city3");
    institutionService.update(institution2);
    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .displayOnNHCPortal(true)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    // test numberSpecimens
    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .numberSpecimens("100")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    assertEquals(
        0,
        institutionService
            .list(
                InstitutionSearchRequest.builder().numberSpecimens("98").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        1,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .numberSpecimens("* , 100")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    assertEquals(
        2,
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .numberSpecimens("97,300")
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    assertEquals(
        institution1.getKey(),
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .get(0)
            .getKey());

    assertEquals(
        institution2.getKey(),
        institutionService
            .list(
                InstitutionSearchRequest.builder()
                    .sortBy(CollectionsSortField.NUMBER_SPECIMENS)
                    .sortOrder(SortOrder.DESC)
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .get(0)
            .getKey());

    institutionService.delete(key2);
    assertEquals(
        0,
        institutionService
            .list(InstitutionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // list by contacts
    Contact contact1 = new Contact();
    contact1.setFirstName("Name1");
    contact1.setEmail(Collections.singletonList("aa1@aa.com"));
    contact1.setTaxonomicExpertise(Arrays.asList("aves", "fungi"));

    UserId userId1 = new UserId(IdType.OTHER, "12345");
    UserId userId2 = new UserId(IdType.OTHER, "abcde");
    contact1.setUserIds(Arrays.asList(userId1, userId2));
    institutionService.addContactPerson(institution1.getKey(), contact1);

    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().query("Name1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().query("aa1@aa.com").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().query("aves").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    assertEquals(
        1,
        institutionService
            .list(InstitutionSearchRequest.builder().query("abcde").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
  }

  @Test
  public void testSuggest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("II");
    institution1.setName("Institution name");
    institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("II2");
    institution2.setName("Institution name2");
    institutionService.create(institution2);

    Institution institution3 = testData.newEntity();
    institution3.setCode("II3");
    institution3.setName("Institution name3");
    UUID key3 = institutionService.create(institution3);
    institutionService.delete(key3);

    assertEquals(2, institutionService.suggest("institution").size());
    assertEquals(2, institutionService.suggest("II").size());
    assertEquals(1, institutionService.suggest("II2").size());
    assertEquals(1, institutionService.suggest("name2").size());
  }

  @Test
  public void listDeletedTest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("code1");
    institution1.setName("Institution name");
    UUID key1 = institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("code2");
    institution2.setName("Institution name2");
    UUID key2 = institutionService.create(institution2);

    assertEquals(0, institutionService.listDeleted(null).getResults().size());

    institutionService.delete(key1);
    assertEquals(1, institutionService.listDeleted(null).getResults().size());

    institutionService.delete(key2);
    assertEquals(2, institutionService.listDeleted(null).getResults().size());

    Institution institution3 = testData.newEntity();
    institution3.setCode("code3");
    institution3.setName("Institution name");
    UUID key3 = institutionService.create(institution3);

    Institution institution4 = testData.newEntity();
    institution4.setCode("code4");
    institution4.setName("Institution name4");
    UUID key4 = institutionService.create(institution4);

    InstitutionSearchRequest searchRequest = new InstitutionSearchRequest();
    searchRequest.setReplacedBy(key4);
    assertEquals(0, institutionService.listDeleted(searchRequest).getResults().size());
    institutionService.replace(key3, key4);
    assertEquals(1, institutionService.listDeleted(searchRequest).getResults().size());
  }

  @Test
  public void listWithoutParametersTest() {
    Institution institution1 = testData.newEntity();
    institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institutionService.create(institution2);

    Institution institution3 = testData.newEntity();
    UUID key3 = institutionService.create(institution3);

    PagingResponse<Institution> response =
        institutionService.list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    institutionService.delete(key3);

    response =
        institutionService.list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().page(new PagingRequest(0L, 1)).build());
    assertEquals(1, response.getResults().size());

    response =
        institutionService.list(
            InstitutionSearchRequest.builder().page(new PagingRequest(0L, 0)).build());
    assertEquals(0, response.getResults().size());
  }

  @Test
  public void createInstitutionWithoutCodeTest() {
    Institution i = testData.newEntity();
    i.setCode(null);
    assertThrows(ValidationException.class, () -> institutionService.create(i));
  }

  @Test
  public void updateInstitutionWithoutCodeTest() {
    Institution i = testData.newEntity();
    UUID key = institutionService.create(i);

    Institution created = institutionService.get(key);
    created.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));
  }

  @Test
  public void updateAndReplaceTest() {
    Institution i = testData.newEntity();
    UUID key = institutionService.create(i);

    Institution created = institutionService.get(key);
    created.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));

    created.setReplacedBy(null);
    created.setConvertedToCollection(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> institutionService.update(created));
  }

  @Test
  public void possibleDuplicatesTest() {
    testDuplicatesCommonCases();
  }

  @Test
  public void invalidEmailsTest() {
    Institution institution = new Institution();
    institution.setCode("cc");
    institution.setName("n1");
    institution.setEmail(Collections.singletonList("asfs"));

    assertThrows(ConstraintViolationException.class, () -> institutionService.create(institution));

    institution.setEmail(Collections.singletonList("aa@aa.com"));
    UUID key = institutionService.create(institution);
    Institution institutionCreated = institutionService.get(key);

    institutionCreated.getEmail().add("asfs");
    assertThrows(
        ConstraintViolationException.class, () -> institutionService.update(institutionCreated));
  }

  @Test
  public void createFromOrganizationTest() {
    Organization organization = createOrganization();

    org.gbif.api.model.registry.Contact orgContact = new org.gbif.api.model.registry.Contact();
    orgContact.setFirstName("firstName");
    orgContact.setLastName("lastName");
    organizationService.addContact(organization.getKey(), orgContact);

    org.gbif.api.model.registry.Contact orgContact2 = new org.gbif.api.model.registry.Contact();
    orgContact2.setFirstName("c2");
    orgContact2.setType(ContactType.PROGRAMMER);
    organizationService.addContact(organization.getKey(), orgContact2);

    String institutionCode = "CODE";
    UUID institutionKey =
        institutionService.createFromOrganization(organization.getKey(), institutionCode);
    Institution institution = institutionService.get(institutionKey);

    assertEquals(institutionCode, institution.getCode());
    assertEquals(MasterSourceType.GBIF_REGISTRY, institution.getMasterSource());

    assertEquals(Source.ORGANIZATION, institution.getMasterSourceMetadata().getSource());
    assertEquals(
        organization.getKey().toString(), institution.getMasterSourceMetadata().getSourceId());
    assertEquals(1, institution.getContactPersons().size());
  }

  @Test
  public void lockMasterSourceFieldsTest() {
    int numberSpecimensOriginal = 23;
    String descriptionOriginal = "description";
    URI homepageOriginal = URI.create("http://test.com");

    Institution institution = new Institution();
    institution.setCode("code");
    institution.setName("name");
    institution.setDescription(descriptionOriginal);
    institution.setNumberSpecimens(numberSpecimensOriginal);
    institution.setHomepage(homepageOriginal);

    Address mailingAddress = new Address();
    mailingAddress.setAddress("safsf");
    mailingAddress.setCountry(Country.AFGHANISTAN);
    institution.setMailingAddress(mailingAddress);

    UUID institutionKey = institutionService.create(institution);

    Contact myContact = new Contact();
    myContact.setFirstName("myContact");
    int contactKey = institutionService.addContactPerson(institutionKey, myContact);

    int metadataKey =
        institutionService.addMasterSourceMetadata(
            institutionKey, new MasterSourceMetadata(Source.IH_IRN, UUID.randomUUID().toString()));

    institution.setNumberSpecimens(12);
    institution.setDescription("aaa");
    institution.setHomepage(URI.create("http://aaaa.com"));

    Address address = new Address();
    address.setAddress("safsf");
    address.setCountry(Country.AFGHANISTAN);
    institution.setAddress(address);

    institutionService.update(institution);

    Institution updatedInstitution = institutionService.get(institutionKey);

    assertNotEquals(numberSpecimensOriginal, updatedInstitution.getNumberSpecimens());
    assertNotEquals(descriptionOriginal, updatedInstitution.getDescription());
    assertEquals(homepageOriginal, updatedInstitution.getHomepage());
    assertNotNull(updatedInstitution.getMailingAddress());
    assertNull(updatedInstitution.getAddress());

    Contact contact = new Contact();
    contact.setFirstName("sfs");
    assertThrows(
        IllegalArgumentException.class,
        () -> institutionService.addContactPerson(institutionKey, contact));

    assertThrows(
        IllegalArgumentException.class,
        () -> institutionService.updateContactPerson(institutionKey, myContact));

    assertThrows(
        IllegalArgumentException.class,
        () -> institutionService.removeContactPerson(institutionKey, contactKey));

    assertDoesNotThrow(
        () ->
            institutionService.addIdentifier(
                institutionKey, new Identifier(IdentifierType.UNKNOWN, "sadf")));

    assertThrows(IllegalArgumentException.class, () -> institutionService.delete(institutionKey));

    // change the master source to GRSCICOLL
    institutionService.deleteMasterSourceMetadata(institutionKey);
    updatedInstitution = institutionService.get(institutionKey);
    assertEquals(MasterSourceType.GRSCICOLL, updatedInstitution.getMasterSource());

    assertDoesNotThrow(() -> institutionService.addContactPerson(institutionKey, contact));
    assertDoesNotThrow(() -> institutionService.updateContactPerson(institutionKey, myContact));
    assertDoesNotThrow(() -> institutionService.removeContactPerson(institutionKey, contactKey));

    // change the master source to GBIF_REGISTRY
    Organization organization = createOrganization();
    metadataKey =
        institutionService.addMasterSourceMetadata(
            institutionKey,
            new MasterSourceMetadata(Source.ORGANIZATION, organization.getKey().toString()));
    updatedInstitution = institutionService.get(institutionKey);
    assertEquals(MasterSourceType.GBIF_REGISTRY, updatedInstitution.getMasterSource());

    String currentDescription = updatedInstitution.getDescription();
    updatedInstitution.setDescription("another one");
    institutionService.update(updatedInstitution);
    updatedInstitution = institutionService.get(institutionKey);
    // the description must remain the same
    assertEquals(currentDescription, updatedInstitution.getDescription());

    // delete the master source
    institutionService.deleteMasterSourceMetadata(institutionKey);
    assertDoesNotThrow(() -> institutionService.delete(institutionKey));
  }

  @Test
  public void listAsGeoJsonTest() {
    Institution institution1 = testData.newEntity();
    institution1.setCode("c1");
    institution1.setName("n1");
    institution1.setLatitude(new BigDecimal(12));
    institution1.setLongitude(new BigDecimal(2));
    UUID key1 = institutionService.create(institution1);

    Institution institution2 = testData.newEntity();
    institution2.setCode("c2");
    institution2.setName("n2");
    institution2.setLatitude(new BigDecimal(23));
    institution2.setLongitude(new BigDecimal(70));
    UUID key2 = institutionService.create(institution2);

    Institution institution3 = testData.newEntity();
    institution3.setCode("c3");
    institution3.setName("n3");
    UUID key3 = institutionService.create(institution3);

    assertEquals(
        2,
        institutionService
            .listGeojson(InstitutionSearchRequest.builder().build())
            .getFeatures()
            .size());
    FeatureCollection featuresC1 =
        institutionService.listGeojson(InstitutionSearchRequest.builder().code("c1").build());
    assertEquals(1, featuresC1.getFeatures().size());
    assertTrue(featuresC1.getFeatures().get(0).getGeometry() instanceof Point);
    assertEquals(2, featuresC1.getFeatures().get(0).getProperties().size());
    assertEquals("n1", featuresC1.getFeatures().get(0).getProperty("name"));
    assertEquals(
        12d,
        ((Point) featuresC1.getFeatures().get(0).getGeometry()).getCoordinates().getLatitude());
    assertEquals(
        2d,
        ((Point) featuresC1.getFeatures().get(0).getGeometry()).getCoordinates().getLongitude());
  }
}
