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
package org.gbif.registry.security.grscicoll;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.merge.ConvertToCollectionParams;
import org.gbif.api.model.collections.merge.MergeParams;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.collections.ChangeSuggestionMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeSuggestionDto;
import org.gbif.registry.security.AuthenticationFacade;
import org.gbif.registry.security.UserRoles;
import org.gbif.ws.WebApplicationException;
import org.gbif.ws.server.GbifHttpServletRequestWrapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrSciCollEditorAuthorizationFilterTest {

  private static final UUID INST_KEY = UUID.randomUUID();
  private static final UUID COLL_KEY = UUID.randomUUID();
  private static final int IH_IDENTIFIER_KEY = 1;
  private static final int LSID_IDENTIFIER_KEY = 2;
  private static final Institution INSTITUTION = new Institution();
  private static final Collection COLLECTION = new Collection();
  private static final Country COUNTRY = Country.SPAIN;
  private static final String NAMESPACE = "ns.gbif.org";
  private static final MachineTag MACHINE_TAG = new MachineTag(NAMESPACE, "test", "value");
  private static final int MACHINE_TAG_KEY = 1;
  private static final Identifier IH_IDENTIFIER = new Identifier(IdentifierType.IH_IRN, "IH");
  private static final Identifier LSID_IDENTIFIER = new Identifier(IdentifierType.LSID, "LSID");
  private static final String USERNAME = "user";
  private static final List<GrantedAuthority> ROLES_GRSCICOLL_EDITOR_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.GRSCICOLL_EDITOR_ROLE));
  private static final List<GrantedAuthority> ROLES_GRSCICOLL_MEDIATOR_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.GRSCICOLL_MEDIATOR_ROLE));
  private static final List<GrantedAuthority> ROLES_GRSCICOLL_ADMIN_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.GRSCICOLL_ADMIN_ROLE));
  private static final List<GrantedAuthority> ROLES_USER_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.USER_ROLE));
  private static final List<GrantedAuthority> ROLES_GRSCICOLL_ADMIN_AND_EDITOR =
      Arrays.asList(
          new SimpleGrantedAuthority(UserRoles.GRSCICOLL_EDITOR_ROLE),
          new SimpleGrantedAuthority(UserRoles.GRSCICOLL_ADMIN_ROLE));
  private static final List<GrantedAuthority> ROLES_EMPTY = Collections.emptyList();

  static {
    IH_IDENTIFIER.setKey(IH_IDENTIFIER_KEY);
    LSID_IDENTIFIER.setKey(LSID_IDENTIFIER_KEY);

    Address address = new Address();
    address.setKey(1);
    address.setCountry(COUNTRY);
    COLLECTION.setKey(COLL_KEY);
    COLLECTION.setCode(UUID.randomUUID().toString());
    COLLECTION.setName(UUID.randomUUID().toString());
    COLLECTION.setInstitutionKey(INST_KEY);
    COLLECTION.setAddress(address);
    INSTITUTION.setKey(INST_KEY);
    INSTITUTION.setCode(UUID.randomUUID().toString());
    INSTITUTION.setName(UUID.randomUUID().toString());
    INSTITUTION.setAddress(address);
  }

  private final GbifHttpServletRequestWrapper mockRequest =
      Mockito.mock(GbifHttpServletRequestWrapper.class);
  private final HttpServletResponse mockResponse = Mockito.mock(HttpServletResponse.class);
  private final FilterChain mockFilterChain = Mockito.mock(FilterChain.class);
  private final AuthenticationFacade mockAuthenticationFacade =
      Mockito.mock(AuthenticationFacade.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final UserRightsMapper mockUserRightsMapper = Mockito.mock(UserRightsMapper.class);
  private final CollectionMapper mockCollectionMapper = Mockito.mock(CollectionMapper.class);
  private final InstitutionMapper mockInstitutionMapper = Mockito.mock(InstitutionMapper.class);
  private final PersonMapper mockPersonMapper = Mockito.mock(PersonMapper.class);
  private final ChangeSuggestionMapper changeSuggestionMapper =
      Mockito.mock(ChangeSuggestionMapper.class);
  private final Authentication mockAuthentication = Mockito.mock(Authentication.class);

  private final GrSciCollAuthorizationService authService =
      new GrSciCollAuthorizationService(
          mockUserRightsMapper,
          mockCollectionMapper,
          mockInstitutionMapper,
          mockPersonMapper,
          changeSuggestionMapper,
          objectMapper);

  private final GrSciCollEditorAuthorizationFilter filter =
      new GrSciCollEditorAuthorizationFilter(authService, mockAuthenticationFacade, objectMapper);

  @Test
  public void ignoreGetRequestsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    when(mockRequest.getMethod()).thenReturn("GET");

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void ignoreOptionsRequestsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    when(mockRequest.getMethod()).thenReturn("OPTIONS");

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void ignoreNonGrSciCollRequestsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/dataset");
    when(mockRequest.getMethod()).thenReturn("POST");

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void addIdentifierAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY.toString() + "/identifier");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1, \"type\": \"IH_IRN\" }");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void addIdentifierAsEditorWithScopeTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY.toString() + "/identifier");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1, \"type\": \"IH_IRN\" }");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void deleteIdentifierAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn(
            "/grscicoll/institution/"
                + INST_KEY.toString()
                + "/identifier/"
                + IH_IDENTIFIER.getKey());
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(Collections.singletonList(IH_IDENTIFIER))
        .when(mockInstitutionMapper)
        .listIdentifiers(INST_KEY);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void deleteIdentifierAsEditorWithScopeTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn(
            "/grscicoll/institution/"
                + INST_KEY.toString()
                + "/identifier/"
                + IH_IDENTIFIER.getKey());
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void addMachineTagAsEditorTest() throws JsonProcessingException {
    // GIVEN
    mockAddMachineTagInstitution(ROLES_GRSCICOLL_EDITOR_ONLY, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void addMachineTagAsEditorWithNamespaceButNotEntityRightsTest()
      throws JsonProcessingException {
    // GIVEN
    mockAddMachineTagInstitution(ROLES_GRSCICOLL_EDITOR_ONLY, true, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void addMachineTagAsEditorWithNamespaceAndEntityRightsTest()
      throws JsonProcessingException {
    // GIVEN
    mockAddMachineTagInstitution(ROLES_GRSCICOLL_EDITOR_ONLY, true, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void deleteMachineTagAsEditorTest() {
    // GIVEN
    mockDeleteMachineTagInstitution(ROLES_GRSCICOLL_EDITOR_ONLY, true, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void deleteMachineTagAsEditorWithNamespaceButNotEntityRightsTest() {
    // GIVEN
    mockDeleteMachineTagInstitution(ROLES_GRSCICOLL_EDITOR_ONLY, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void deleteMachineTagAsEditorWithNamespaceAndEntityRightsTest() {
    // GIVEN
    mockDeleteMachineTagInstitution(ROLES_GRSCICOLL_EDITOR_ONLY, true, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionCreationAsAdminTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionCreation(ROLES_GRSCICOLL_ADMIN_ONLY, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionCreationAsEditorAndCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionCreation(ROLES_GRSCICOLL_EDITOR_ONLY, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionCreationAsMediatorAndCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionCreation(ROLES_GRSCICOLL_MEDIATOR_ONLY, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionCreationAsUserTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionCreation(ROLES_USER_ONLY, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void collectionCreationAsEditorTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionCreation(ROLES_GRSCICOLL_EDITOR_ONLY, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void collectionCreationAsEditorAndInstitutionRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionCreation(ROLES_GRSCICOLL_EDITOR_ONLY, true, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionCreationAsEditorAndCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionCreation(ROLES_GRSCICOLL_EDITOR_ONLY, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionCreationAsMediatorAndCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionCreation(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionUpdateAsEditorWithEntityRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, true, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionUpdateAsEditorWithCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionUpdateAsMediatorWithCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionUpdate(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionUpdateAsEditorWithNoRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void addInstitutionCommentAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY + "/comment");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"content\": \"comment\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void addInstitutionCommentAsEditorWithNoRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY + "/comment");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"content\": \"comment\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(false).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void institutionDeletionAsEditorTest() {
    // GIVEN
    mockInstitutionDeletion(ROLES_GRSCICOLL_EDITOR_ONLY, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void institutionDeletionAsMediatorTest() {
    // GIVEN
    mockInstitutionDeletion(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void institutionDeletionAsMediatorWithCountryRightsTest() {
    // GIVEN
    mockInstitutionDeletion(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionDeletionAsMediatorWithEntityRightsTest() {
    // GIVEN
    mockInstitutionDeletion(ROLES_GRSCICOLL_MEDIATOR_ONLY, true, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionUpdateAsEditorWithCollectionRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, true, false, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionUpdateAsEditorWithInstitutionRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, false, true, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionUpdateAsEditorWithCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, false, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionUpdateAsMediatorWithCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionUpdate(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionUpdateAsEditorWithNoRightsTest() throws JsonProcessingException {
    // GIVEN
    mockCollectionUpdate(ROLES_GRSCICOLL_EDITOR_ONLY, false, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void collectionDeletionAsEditorTest() {
    // GIVEN
    mockCollectionDeletion(ROLES_GRSCICOLL_EDITOR_ONLY, false, false);

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionDeletionAsMediatorTest() {
    // GIVEN
    mockCollectionDeletion(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, false);

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionDeletionAsMediatorWithEntityRightsTest() {
    // GIVEN
    mockCollectionDeletion(ROLES_GRSCICOLL_MEDIATOR_ONLY, true, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionDeletionAsMediatorWithCountryRightsTest() {
    // GIVEN
    mockCollectionDeletion(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void collectionDeletionAsEditorWithCountryRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(COLLECTION).when(mockCollectionMapper).get(COLL_KEY);
    doReturn(false).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
    doReturn(true)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void changeInstitutionKeyInCollectionAsEditorWithNoRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");

    // the institution key is changed in the update
    UUID anotherInstKey = UUID.randomUUID();
    when(mockRequest.getContent())
        .thenReturn(
            "{\"key\": \"" + COLL_KEY + "\", \"institutionKey\": \"" + anotherInstKey + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // we return the old institution key in the mapepr
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, anotherInstKey);
    doReturn(false).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void changeInstitutionKeyInCollectionAsEditorWithRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");

    // the institution key is changed in the update
    UUID anotherInstKey = UUID.randomUUID();
    when(mockRequest.getContent())
        .thenReturn(
            "{\"key\": \"" + COLL_KEY + "\", \"institutionKey\": \"" + anotherInstKey + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(COLLECTION).when(mockCollectionMapper).get(COLL_KEY);

    // we return the old institution key in the mapper
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, anotherInstKey);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void addCollectionCommentAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY + "/comment");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"content\": \"comment\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(COLLECTION).when(mockCollectionMapper).get(COLL_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void createPersonAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/person/");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": " + UUID.randomUUID() + "}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void createChangeSuggestionNotLoggedTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateChangeSuggestionAsAdminTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_ADMIN_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateNewEntityChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.CREATE);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateModifyEntityChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.UPDATE);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateDeleteEntityChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.DELETE);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateMergeEntityChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.MERGE);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateConvertToCollectionEntityChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.CONVERSION_TO_COLLECTION);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void applyChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/changeSuggestion/1/apply");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.CONVERSION_TO_COLLECTION);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void discardChangeSuggestionAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/changeSuggestion/1/discard");
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);

    ChangeSuggestionDto dto = new ChangeSuggestionDto();
    dto.setType(Type.CONVERSION_TO_COLLECTION);
    when(changeSuggestionMapper.get(1)).thenReturn(dto);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN, THEN
    assertThrows(
        WebApplicationException.class,
        () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void mergeCollectionAsEditorTest() throws JsonProcessingException {
    // GIVEN
    mockMergeCollection(ROLES_GRSCICOLL_EDITOR_ONLY, true, true, true);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void mergeCollectionAsMediatorAndRightsOnSourceTest() throws JsonProcessingException {
    // GIVEN
    mockMergeCollection(ROLES_GRSCICOLL_MEDIATOR_ONLY, true, false, true);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void mergeCollectionAsMediatorAndRightsOnSourceAndTargetTest()
      throws JsonProcessingException {
    // GIVEN
    mockMergeCollection(ROLES_GRSCICOLL_MEDIATOR_ONLY, true, true, true);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void convertInstitutionAsEditorTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionConversion(ROLES_GRSCICOLL_EDITOR_ONLY, true, true, true);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void convertInstitutionAsMediatorWithNoRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionConversion(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void convertInstitutionAsMediatorWithEntityRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionConversion(ROLES_GRSCICOLL_MEDIATOR_ONLY, true, false, false);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void convertInstitutionAsMediatorWithCountryRightsTest() throws JsonProcessingException {
    // GIVEN
    mockInstitutionConversion(ROLES_GRSCICOLL_MEDIATOR_ONLY, false, false, true);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void convertInstitutionAsMediatorWithEntityAndTargetRightsTest()
      throws JsonProcessingException {
    // GIVEN
    mockInstitutionConversion(ROLES_GRSCICOLL_MEDIATOR_ONLY, true, true, false);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  private void mockInstitutionConversion(
      List<GrantedAuthority> roles,
      boolean institutionRights,
      boolean targetInstitutionRights,
      boolean countryRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY + "/convertToCollection");
    when(mockRequest.getMethod()).thenReturn("PUT");

    ConvertToCollectionParams params = new ConvertToCollectionParams();
    params.setInstitutionForNewCollectionKey(UUID.randomUUID());
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(params));
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);

    Institution institutionForConvertedCollection = new Institution();
    institutionForConvertedCollection.setKey(params.getInstitutionForNewCollectionKey());
    institutionForConvertedCollection.setCode(UUID.randomUUID().toString());
    institutionForConvertedCollection.setName(UUID.randomUUID().toString());
    doReturn(institutionForConvertedCollection)
        .when(mockInstitutionMapper)
        .get(params.getInstitutionForNewCollectionKey());

    doReturn(institutionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
    doReturn(targetInstitutionRights)
        .when(mockUserRightsMapper)
        .keyExistsForUser(USERNAME, params.getInstitutionForNewCollectionKey());
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockInstitutionUpdate(
      List<GrantedAuthority> roles, boolean institutionRights, boolean countryRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(INSTITUTION));
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(institutionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockCollectionUpdate(
      List<GrantedAuthority> roles,
      boolean collectionRights,
      boolean institutionRights,
      boolean countryRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(COLLECTION));
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(COLLECTION).when(mockCollectionMapper).get(COLL_KEY);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(collectionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
    doReturn(institutionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockInstitutionCreation(List<GrantedAuthority> roles, boolean countryRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(INSTITUTION));
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockCollectionCreation(
      List<GrantedAuthority> roles, boolean collectionRights, boolean countryRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(COLLECTION));
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(collectionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockCollectionDeletion(
      List<GrantedAuthority> roles, boolean collectionRights, boolean countryRights) {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(COLLECTION).when(mockCollectionMapper).get(COLL_KEY);
    doReturn(collectionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockInstitutionDeletion(
      List<GrantedAuthority> roles, boolean institutionRights, boolean countryRights) {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY);
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(institutionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }

  private void mockAddMachineTagInstitution(
      List<GrantedAuthority> roles, boolean nsRights, boolean institutionRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY.toString() + "/machineTag");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(MACHINE_TAG));
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(nsRights)
        .when(mockUserRightsMapper)
        .namespaceExistsForUser(USERNAME, MACHINE_TAG.getNamespace());
    doReturn(institutionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
    doReturn(roles).when(mockAuthentication).getAuthorities();
  }

  private void mockDeleteMachineTagInstitution(
      List<GrantedAuthority> roles, boolean nsRights, boolean institutionRights) {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn(
            "/grscicoll/institution/" + INST_KEY.toString() + "/machineTag/" + MACHINE_TAG_KEY);
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(INSTITUTION).when(mockInstitutionMapper).get(INST_KEY);
    doReturn(nsRights)
        .when(mockUserRightsMapper)
        .allowedToDeleteMachineTag(USERNAME, MACHINE_TAG_KEY);
    doReturn(institutionRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);
  }

  private void mockMergeCollection(
      List<GrantedAuthority> roles,
      boolean sourceRights,
      boolean targetRights,
      boolean countryRights)
      throws JsonProcessingException {
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY + "/merge");
    when(mockRequest.getMethod()).thenReturn("POST");
    MergeParams mergeParams = new MergeParams();
    mergeParams.setReplacementEntityKey(UUID.randomUUID());
    when(mockRequest.getContent()).thenReturn(objectMapper.writeValueAsString(mergeParams));
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(roles).when(mockAuthentication).getAuthorities();
    doReturn(COLLECTION).when(mockCollectionMapper).get(COLL_KEY);

    Collection targetEntity = new Collection();
    targetEntity.setKey(mergeParams.getReplacementEntityKey());
    targetEntity.setCode(UUID.randomUUID().toString());
    targetEntity.setName(UUID.randomUUID().toString());
    doReturn(targetEntity).when(mockCollectionMapper).get(mergeParams.getReplacementEntityKey());

    doReturn(sourceRights).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
    doReturn(targetRights)
        .when(mockUserRightsMapper)
        .keyExistsForUser(USERNAME, mergeParams.getReplacementEntityKey());
    doReturn(countryRights)
        .when(mockUserRightsMapper)
        .countryExistsForUser(USERNAME, COUNTRY.getIso2LetterCode());
  }
}
