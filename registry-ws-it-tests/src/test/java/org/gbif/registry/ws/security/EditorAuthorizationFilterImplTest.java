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
package org.gbif.registry.ws.security;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.common.shaded.com.google.common.collect.ImmutableMap;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.gbif.registry.security.AuthenticationFacade;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.EditorAuthorizationServiceImpl;
import org.gbif.registry.security.UserRoles;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditorAuthorizationFilterImplTest {

  private final Installation denmarkInstallation = new Installation();
  private final Dataset denmarkDataset = new Dataset();
  private final Organization denmarkOrganization = new Organization();
  private final Node denmarkNode = new Node();

  private final String nothingEditor = "nothingEditor";
  private final String datasetEditor = "datasetEditor";
  private final String installationEditor = "installationEditor";
  private final String organizationEditor = "organizationEditor";
  private final String nodeEditor = "nodeEditor";

  private Map<String, UUID> editorRights;

  @Mock ServletContext mockContext;

  @Mock HttpServletRequest mockRequest;

  @Mock HttpServletResponse mockResponse;

  @Mock FilterChain filterChain;

  @Mock DatasetMapper datasetMapper;

  @Mock InstallationMapper installationMapper;

  @Mock OrganizationMapper organizationMapper;

  @Mock UserRightsMapper userRightsMapper;

  private EditorAuthorizationFilter filter;

  private EditorAuthorizationServiceImpl editorAuthorizationService;

  @Autowired private AuthenticationFacade authenticationFacade;

  @Autowired private ObjectMapper objectMapper;

  @Before
  public void setupMocks() {
    denmarkDataset.setKey(UUID.randomUUID());
    denmarkInstallation.setKey(UUID.randomUUID());
    denmarkOrganization.setKey(UUID.randomUUID());
    denmarkNode.setKey(UUID.randomUUID());

    denmarkDataset.setInstallationKey(denmarkInstallation.getKey());
    denmarkDataset.setPublishingOrganizationKey(denmarkOrganization.getKey());

    denmarkInstallation.setOrganizationKey(denmarkOrganization.getKey());

    denmarkOrganization.setEndorsingNodeKey(denmarkNode.getKey());

    editorRights =
        ImmutableMap.of(
            datasetEditor, denmarkDataset.getKey(),
            installationEditor, denmarkInstallation.getKey(),
            organizationEditor, denmarkOrganization.getKey(),
            nodeEditor, denmarkNode.getKey());

    editorAuthorizationService =
        new EditorAuthorizationServiceImpl(
            organizationMapper, datasetMapper, installationMapper, userRightsMapper);

    // setup filter with mocks
    filter =
        new EditorAuthorizationFilter(
            editorAuthorizationService, authenticationFacade, objectMapper);
    filter.setServletContext(mockContext);
    when(mockRequest.isUserInRole(not(eq(UserRoles.EDITOR_ROLE)))).thenReturn(false);
    when(mockRequest.isUserInRole(eq(UserRoles.EDITOR_ROLE))).thenReturn(true);

    // setup mocks to authorize based on editorRights map.
    when(userRightsMapper.keyExistsForUser(Matchers.any(), Matchers.any()))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  return editorRights.containsKey(args[0])
                      && editorRights.get(args[0]).equals(args[1]);
                });

    when(datasetMapper.get(denmarkDataset.getKey())).thenReturn(denmarkDataset);

    when(installationMapper.get(denmarkInstallation.getKey())).thenReturn(denmarkInstallation);
    when(organizationMapper.get(denmarkOrganization.getKey())).thenReturn(denmarkOrganization);
  }

  /**
   * The user has permission to edit
   *
   * @throws Exception
   */
  @Test
  public void testGoodCases() throws Exception {
    setRequestUser(datasetEditor);
    mockRequest("DELETE", "dataset/" + denmarkDataset.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);

    setRequestUser(installationEditor);
    mockRequest("DELETE", "dataset/" + denmarkDataset.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
    mockRequest("DELETE", "installation/" + denmarkInstallation.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);

    setRequestUser(organizationEditor);
    mockRequest("DELETE", "dataset/" + denmarkDataset.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
    mockRequest("DELETE", "installation/" + denmarkInstallation.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
    mockRequest("DELETE", "organization/" + denmarkOrganization.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);

    setRequestUser(nodeEditor);
    mockRequest("DELETE", "dataset/" + denmarkDataset.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
    mockRequest("DELETE", "installation/" + denmarkInstallation.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
    mockRequest("DELETE", "organization/" + denmarkOrganization.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
    mockRequest("DELETE", "node/" + denmarkNode.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
  }

  @Test(expected = WebApplicationException.class)
  public void testNoRights() throws Exception {
    setRequestUser(nothingEditor);
    mockRequest("DELETE", "dataset/" + denmarkDataset.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
  }

  @Test(expected = WebApplicationException.class)
  public void testOnlyDatasetRights() throws Exception {
    setRequestUser(datasetEditor);
    mockRequest("DELETE", "installation/" + denmarkInstallation.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
  }

  @Test(expected = WebApplicationException.class)
  public void testOnlyDatasetRights2() throws Exception {
    setRequestUser(datasetEditor);
    mockRequest("DELETE", "organization/" + denmarkOrganization.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
  }

  @Test(expected = WebApplicationException.class)
  public void testOnlyOrganizationRights() throws Exception {
    setRequestUser(organizationEditor);
    mockRequest("DELETE", "node/" + denmarkNode.getKey().toString());
    filter.doFilter(mockRequest, mockResponse, filterChain);
  }

  /* Entity creation */

  @Test
  public void testCreateGoodCases() throws Exception {
    Assert.assertTrue(
        editorAuthorizationService.allowedToModifyEntity(
            organizationEditor, denmarkDataset.getPublishingOrganizationKey()));
    Assert.assertTrue(
        editorAuthorizationService.allowedToModifyEntity(
            nodeEditor, denmarkOrganization.getEndorsingNodeKey()));
  }

  @Test
  public void testCreateFailCases() throws Exception {
    Assert.assertFalse(
        editorAuthorizationService.allowedToModifyEntity(nothingEditor, denmarkDataset.getKey()));
    Assert.assertFalse(
        editorAuthorizationService.allowedToModifyEntity(
            datasetEditor, denmarkDataset.getPublishingOrganizationKey()));
    Assert.assertFalse(
        editorAuthorizationService.allowedToModifyEntity(
            installationEditor, denmarkInstallation.getOrganizationKey()));
    Assert.assertFalse(
        editorAuthorizationService.allowedToModifyEntity(
            organizationEditor, denmarkOrganization.getEndorsingNodeKey()));
  }

  private void mockRequest(String method, String path) {
    when(mockRequest.getRequestURI()).thenReturn(path);
    when(mockRequest.getMethod()).thenReturn(method.toUpperCase().trim());
  }

  private void setRequestUser(String user) {
    when(mockRequest.getUserPrincipal()).thenReturn(principal(user));
  }

  private Principal principal(final String user) {
    return () -> user;
  }
}
