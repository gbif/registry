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

import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;
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
  private static final Identifier IH_IDENTIFIER = new Identifier(IdentifierType.IH_IRN, "IH");
  private static final Identifier LSID_IDENTIFIER = new Identifier(IdentifierType.LSID, "LSID");
  private static final String USERNAME = "user";
  private static final List<GrantedAuthority> ROLES_GRSCICOLL_EDITOR_ONLY =
      Collections.singletonList(new SimpleGrantedAuthority(UserRoles.GRSCICOLL_EDITOR_ROLE));
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
  private final Authentication mockAuthentication = Mockito.mock(Authentication.class);

  private final GrSciCollEditorAuthorizationService authService =
      new GrSciCollEditorAuthorizationService(
          mockUserRightsMapper, mockCollectionMapper, mockInstitutionMapper, mockPersonMapper);

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
  public void addIrnIdentifierAsEditorTest() {
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
  public void deleteIrnIdentifierAsEditorTest() {
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
  public void addNonIrnIdentifierAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY.toString() + "/identifier");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": 1, \"type\": \"LSID\" }");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void deleteNonIrnIdentifierAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY.toString() + "/identifier/1");
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(Collections.singletonList(LSID_IDENTIFIER))
        .when(mockInstitutionMapper)
        .listIdentifiers(INST_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void addMachineTagAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI())
        .thenReturn("/grscicoll/institution/" + INST_KEY.toString() + "/machineTag");
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
  public void institutionCreationAsAdminTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_ADMIN_AND_EDITOR).when(mockAuthentication).getAuthorities();
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionCreationAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void institutionCreationAsUserTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_USER_ONLY).when(mockAuthentication).getAuthorities();

    // WHEN
    WebApplicationException ex =
        assertThrows(
            WebApplicationException.class,
            () -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));

    // THEN
    assertEquals(HttpStatus.FORBIDDEN.value(), ex.getStatus());
  }

  @Test
  public void collectionCreationAsEditorAndInstitutionRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent())
        .thenReturn("{\"key\": \"" + COLL_KEY + "\", \"institutionKey\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionUpdateAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void institutionUpdateAsEditorWithNoRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent()).thenReturn("{\"key\": \"" + INST_KEY + "\"}");
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
  public void addInstitutionCommentAsEditorTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY + "/comment");
    when(mockRequest.getMethod()).thenReturn("POST");
    when(mockRequest.getContent()).thenReturn("{\"content\": \"comment\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
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
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/institution/" + INST_KEY);
    when(mockRequest.getMethod()).thenReturn("DELETE");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateCollectionAsEditorWithCollectionRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent())
        .thenReturn("{\"key\": \"" + COLL_KEY + "\", \"institutionKey\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(INST_KEY).when(mockCollectionMapper).getInstitutionKey(COLL_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateCollectionAsEditorWithInstitutionRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent())
        .thenReturn("{\"key\": \"" + COLL_KEY + "\", \"institutionKey\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(INST_KEY).when(mockCollectionMapper).getInstitutionKey(COLL_KEY);
    doReturn(false).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, INST_KEY);

    // WHEN, THEN
    assertDoesNotThrow(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain));
  }

  @Test
  public void updateCollectionAsEditorWithNoRightsTest() {
    // GIVEN
    when(mockAuthenticationFacade.getAuthentication()).thenReturn(mockAuthentication);
    when(mockRequest.getRequestURI()).thenReturn("/grscicoll/collection/" + COLL_KEY);
    when(mockRequest.getMethod()).thenReturn("PUT");
    when(mockRequest.getContent())
        .thenReturn("{\"key\": \"" + COLL_KEY + "\", \"institutionKey\": \"" + INST_KEY + "\"}");
    when(mockAuthentication.getName()).thenReturn(USERNAME);
    doReturn(ROLES_GRSCICOLL_EDITOR_ONLY).when(mockAuthentication).getAuthorities();
    doReturn(INST_KEY).when(mockCollectionMapper).getInstitutionKey(COLL_KEY);
    doReturn(false).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
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
  public void changeInstitutionKeyInCollectionAsEditorTest() {
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
    doReturn(INST_KEY).when(mockCollectionMapper).getInstitutionKey(COLL_KEY);
    doReturn(false).when(mockUserRightsMapper).keyExistsForUser(USERNAME, COLL_KEY);
    doReturn(true).when(mockUserRightsMapper).keyExistsForUser(USERNAME, anotherInstKey);

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
    doReturn(INST_KEY).when(mockCollectionMapper).getInstitutionKey(COLL_KEY);
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
}
