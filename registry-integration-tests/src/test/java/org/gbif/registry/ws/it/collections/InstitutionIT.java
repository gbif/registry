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
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.collections.duplicates.Duplicate;
import org.gbif.api.model.collections.duplicates.DuplicatesRequest;
import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.client.collections.InstitutionClient;
import org.gbif.registry.ws.resources.collections.InstitutionResource;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.gbif.ws.security.KeyStore;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the {@link InstitutionResource}. */
public class InstitutionIT extends ExtendedCollectionEntityIT<Institution> {

  private static final String NAME = "name";
  private static final String DESCRIPTION = "dummy description";
  private static final URI HOMEPAGE = URI.create("http://dummy");
  private static final String CODE_UPDATED = "code2";
  private static final String NAME_UPDATED = "name2";
  private static final String DESCRIPTION_UPDATED = "dummy description updated";
  private static final String ADDITIONAL_NAME = "additional name";

  @Autowired
  public InstitutionIT(
      InstitutionService institutionResource,
      PersonService personResource,
      DatasetService datasetService,
      NodeService nodeService,
      OrganizationService organizationService,
      InstallationService installationService,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer,
      IdentityService identityService,
      @LocalServerPort int localServerPort,
      KeyStore keyStore) {
    super(
        institutionResource,
        InstitutionClient.class,
        personResource,
        datasetService,
        nodeService,
        organizationService,
        installationService,
        principalProvider,
        esServer,
        identityService,
        localServerPort,
        keyStore);
  }

  @BeforeAll
  public static void createMaterializedViews() throws SQLException {
    Connection connection = database.getTestDatabase().getConnection();
    // create materialized view for testing
    ScriptUtils.executeSqlScript(
        connection, new ClassPathResource("/scripts/create_institution_duplicates_mv.sql"));
    connection.close();
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listTest(ServiceType serviceType) {
    InstitutionService service = ((InstitutionService) getService(serviceType));

    Institution institution1 = newEntity();
    institution1.setCode("c1");
    institution1.setName("n1");
    Address address = new Address();
    address.setAddress("dummy address");
    address.setCity("city");
    address.setCountry(Country.DENMARK);
    institution1.setAddress(address);
    institution1.setAlternativeCodes(Collections.singletonList(new AlternativeCode("alt", "test")));
    UUID key1 = service.create(institution1);

    Institution institution2 = newEntity();
    institution2.setCode("c2");
    institution2.setName("n2");
    Address address2 = new Address();
    address2.setAddress("dummy address2");
    address2.setCity("city2");
    address2.setCountry(Country.SPAIN);
    institution2.setAddress(address2);
    UUID key2 = service.create(institution2);

    PagingResponse<Institution> response =
        service.list(InstitutionSearchRequest.builder().query("dummy").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    // empty queries are ignored and return all elements
    response =
        service.list(InstitutionSearchRequest.builder().query("").page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        service.list(InstitutionSearchRequest.builder().query("city").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key1, response.getResults().get(0).getKey());

    response =
        service.list(InstitutionSearchRequest.builder().query("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());

    // code and name params
    assertEquals(
        1,
        service
            .list(InstitutionSearchRequest.builder().code("c1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        service
            .list(InstitutionSearchRequest.builder().name("n2").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        1,
        service
            .list(
                InstitutionSearchRequest.builder().code("c1").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(
                InstitutionSearchRequest.builder().code("c2").name("n1").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // query param
    assertEquals(
        2,
        service
            .list(InstitutionSearchRequest.builder().query("c").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(InstitutionSearchRequest.builder().query("dum add").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(InstitutionSearchRequest.builder().query("<").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(InstitutionSearchRequest.builder().query("\"<\"").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(InstitutionSearchRequest.builder().query("  ").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    // alternative code
    response =
        service.list(
            InstitutionSearchRequest.builder().alternativeCode("alt").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());

    response =
        service.list(
            InstitutionSearchRequest.builder().alternativeCode("foo").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());

    response =
        service.list(
            InstitutionSearchRequest.builder().country(Country.SPAIN).page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());
    response =
        service.list(
            InstitutionSearchRequest.builder()
                .country(Country.AFGHANISTAN)
                .page(DEFAULT_PAGE)
                .build());
    assertEquals(0, response.getResults().size());

    response =
        service.list(InstitutionSearchRequest.builder().city("city2").page(DEFAULT_PAGE).build());
    assertEquals(1, response.getResults().size());
    assertEquals(key2, response.getResults().get(0).getKey());
    response =
        service.list(InstitutionSearchRequest.builder().city("foo").page(DEFAULT_PAGE).build());
    assertEquals(0, response.getResults().size());

    // update address
    institution2 = service.get(key2);
    assertNotNull(institution2.getAddress());
    institution2.getAddress().setCity("city3");
    service.update(institution2);
    assertEquals(
        1,
        service
            .list(InstitutionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());

    service.delete(key2);
    assertEquals(
        0,
        service
            .list(InstitutionSearchRequest.builder().query("city3").page(DEFAULT_PAGE).build())
            .getResults()
            .size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void testSuggest(ServiceType serviceType) {
    InstitutionService service = ((InstitutionService) getService(serviceType));

    Institution institution1 = newEntity();
    institution1.setCode("II");
    institution1.setName("Institution name");
    service.create(institution1);

    Institution institution2 = newEntity();
    institution2.setCode("II2");
    institution2.setName("Institution name2");
    service.create(institution2);

    assertEquals(2, service.suggest("institution").size());
    assertEquals(2, service.suggest("II").size());
    assertEquals(1, service.suggest("II2").size());
    assertEquals(1, service.suggest("name2").size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listDeletedTest(ServiceType serviceType) {
    InstitutionService service = ((InstitutionService) getService(serviceType));

    Institution institution1 = newEntity();
    institution1.setCode("code1");
    institution1.setName("Institution name");
    UUID key1 = service.create(institution1);

    Institution institution2 = newEntity();
    institution2.setCode("code2");
    institution2.setName("Institution name2");
    UUID key2 = service.create(institution2);

    assertEquals(0, service.listDeleted(DEFAULT_PAGE).getResults().size());

    service.delete(key1);
    assertEquals(1, service.listDeleted(DEFAULT_PAGE).getResults().size());

    service.delete(key2);
    assertEquals(2, service.listDeleted(DEFAULT_PAGE).getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listWithoutParametersTest(ServiceType serviceType) {
    InstitutionService service = (InstitutionService) getService(serviceType);

    Institution institution1 = newEntity();
    service.create(institution1);

    Institution institution2 = newEntity();
    service.create(institution2);

    Institution institution3 = newEntity();
    UUID key3 = service.create(institution3);

    PagingResponse<Institution> response =
        service.list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(3, response.getResults().size());

    service.delete(key3);

    response = service.list(InstitutionSearchRequest.builder().page(DEFAULT_PAGE).build());
    assertEquals(2, response.getResults().size());

    response =
        service.list(InstitutionSearchRequest.builder().page(new PagingRequest(0L, 1)).build());
    assertEquals(1, response.getResults().size());

    response =
        service.list(InstitutionSearchRequest.builder().page(new PagingRequest(0L, 0)).build());
    assertEquals(0, response.getResults().size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void listByContactTest(ServiceType serviceType) {
    InstitutionService service = (InstitutionService) getService(serviceType);
    PersonService personService = getService(serviceType, personResource, personClient);

    // persons
    Person person1 = new Person();
    person1.setFirstName("first name");
    UUID personKey1 = personService.create(person1);

    Person person2 = new Person();
    person2.setFirstName("first name2");
    UUID personKey2 = personService.create(person2);

    // institutions
    Institution institution1 = newEntity();
    UUID instutionKey1 = service.create(institution1);

    Institution institution2 = newEntity();
    UUID instutionKey2 = service.create(institution2);

    // add contacts
    service.addContact(instutionKey1, personKey1);
    service.addContact(instutionKey1, personKey2);
    service.addContact(instutionKey2, personKey2);

    assertEquals(
        1,
        service
            .list(InstitutionSearchRequest.builder().contact(personKey1).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        2,
        service
            .list(InstitutionSearchRequest.builder().contact(personKey2).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
    assertEquals(
        0,
        service
            .list(
                InstitutionSearchRequest.builder()
                    .contact(UUID.randomUUID())
                    .page(DEFAULT_PAGE)
                    .build())
            .getResults()
            .size());

    service.removeContact(instutionKey1, personKey2);
    assertEquals(
        1,
        service
            .list(InstitutionSearchRequest.builder().contact(personKey2).page(DEFAULT_PAGE).build())
            .getResults()
            .size());
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void createInstitutionWithoutCodeTest(ServiceType serviceType) {
    InstitutionService service = (InstitutionService) getService(serviceType);
    Institution i = newEntity();
    i.setCode(null);
    assertThrows(ValidationException.class, () -> service.create(i));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updateInstitutionWithoutCodeTest(ServiceType serviceType) {
    InstitutionService service = (InstitutionService) getService(serviceType);
    Institution i = newEntity();
    service.create(i);

    i.setCode(null);
    assertThrows(IllegalArgumentException.class, () -> service.update(i));
  }

  @ParameterizedTest
  @EnumSource(ServiceType.class)
  public void updateAndReplaceTest(ServiceType serviceType) {
    InstitutionService service = (InstitutionService) getService(serviceType);
    Institution i = newEntity();
    service.create(i);

    i.setReplacedBy(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> service.update(i));

    i.setReplacedBy(null);
    i.setConvertedToCollection(UUID.randomUUID());
    assertThrows(IllegalArgumentException.class, () -> service.update(i));
  }

  @Test
  public void possibleDuplicatesTest() {
    InstitutionClient instClient = (InstitutionClient) client;

    // same code
    DuplicatesRequest request = new DuplicatesRequest();
    request.setSameCode(true);

    DuplicatesResult result = instClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(3, result.getDuplicates().get(0).size());

    // same fuzzy name
    request = new DuplicatesRequest();
    request.setSameFuzzyName(true);

    result = instClient.findPossibleDuplicates(request);
    assertEquals(2, result.getDuplicates().size());
    assertEquals(3, result.getDuplicates().get(0).size());
    assertEquals(2, result.getDuplicates().get(1).size());

    // same fuzzy name and city
    request = new DuplicatesRequest();
    request.setSameFuzzyName(true);
    request.setSameCity(true);

    result = instClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    Set<UUID> keysFound =
        result.getDuplicates().get(0).stream().map(Duplicate::getKey).collect(Collectors.toSet());
    assertEquals(2, keysFound.size());

    // exclude keys
    request.setExcludeKeys(new ArrayList<>(keysFound));
    result = instClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    // same name and city
    request = new DuplicatesRequest();
    request.setSameName(true);
    request.setSameCity(true);
    result = instClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    // same name and not same city
    request.setSameCity(false);
    result = instClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    assertEquals(
        2, result.getDuplicates().get(0).stream().map(Duplicate::getKey).distinct().count());

    // filtering by country
    request.setInCountries(Collections.singletonList(Country.DENMARK.getIso2LetterCode()));
    result = instClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
    assertEquals(
        2, result.getDuplicates().get(0).stream().map(Duplicate::getKey).distinct().count());

    request.setInCountries(Collections.singletonList(Country.SPAIN.getIso2LetterCode()));
    result = instClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    // excluding country
    request.setInCountries(null);
    request.setNotInCountries(Collections.singletonList(Country.DENMARK.getIso2LetterCode()));
    result = instClient.findPossibleDuplicates(request);
    assertTrue(result.getDuplicates().isEmpty());

    request.setNotInCountries(Collections.singletonList(Country.SPAIN.getIso2LetterCode()));
    result = instClient.findPossibleDuplicates(request);
    assertEquals(1, result.getDuplicates().size());
    assertEquals(2, result.getDuplicates().get(0).size());
  }

  @Override
  protected Institution newEntity() {
    Institution institution = new Institution();
    institution.setCode(UUID.randomUUID().toString());
    institution.setName(NAME);
    institution.setDescription(DESCRIPTION);
    institution.setHomepage(HOMEPAGE);
    institution.setAdditionalNames(Collections.emptyList());
    return institution;
  }

  @Override
  protected void assertNewEntity(Institution institution) {
    assertEquals(NAME, institution.getName());
    assertEquals(DESCRIPTION, institution.getDescription());
    assertEquals(HOMEPAGE, institution.getHomepage());
    assertTrue(institution.getAdditionalNames().isEmpty());
  }

  @Override
  protected Institution updateEntity(Institution institution) {
    institution.setCode(CODE_UPDATED);
    institution.setName(NAME_UPDATED);
    institution.setDescription(DESCRIPTION_UPDATED);
    institution.setAdditionalNames(Collections.singletonList(ADDITIONAL_NAME));
    return institution;
  }

  @Override
  protected void assertUpdatedEntity(Institution institution) {
    assertEquals(CODE_UPDATED, institution.getCode());
    assertEquals(NAME_UPDATED, institution.getName());
    assertEquals(DESCRIPTION_UPDATED, institution.getDescription());
    assertEquals(1, institution.getAdditionalNames().size());
    assertNotEquals(institution.getCreated(), institution.getModified());
  }

  @Override
  protected Institution newInvalidEntity() {
    return new Institution();
  }
}
