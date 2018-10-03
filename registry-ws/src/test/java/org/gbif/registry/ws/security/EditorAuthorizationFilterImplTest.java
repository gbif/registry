package org.gbif.registry.ws.security;

import com.sun.jersey.spi.container.ContainerRequest;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.common.shaded.com.google.common.collect.ImmutableMap;
import org.gbif.registry.persistence.mapper.UserRightsMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;

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

  @Mock
  SecurityContext secContext;
  @Mock
  ContainerRequest mockRequest;
  @Mock
  DatasetService datasetService;
  @Mock
  InstallationService installationService;
  @Mock
  OrganizationService organizationService;
  @Mock
  UserRightsMapper userRightsMapper;

  private EditorAuthorizationFilter filter;

  private EditorAuthorizationServiceImpl editorAuthorizationService;

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

    editorRights = ImmutableMap.of(
      datasetEditor, denmarkDataset.getKey(),
      installationEditor, denmarkInstallation.getKey(),
      organizationEditor, denmarkOrganization.getKey(),
      nodeEditor, denmarkNode.getKey()
    );

    editorAuthorizationService = new EditorAuthorizationServiceImpl(datasetService, installationService, organizationService, userRightsMapper);

    // setup filter with mocks
    filter = new EditorAuthorizationFilter(editorAuthorizationService);
    filter.setSecContext(secContext);
    when(secContext.isUserInRole(not(eq(UserRoles.EDITOR_ROLE)))).thenReturn(false);
    when(secContext.isUserInRole(eq(UserRoles.EDITOR_ROLE))).thenReturn(true);

    // setup mocks to authorize based on editorRights map.
    when(userRightsMapper.keyExistsForUser(Matchers.any(), Matchers.any())).thenAnswer((Answer<Boolean>) invocation -> {
      Object[] args = invocation.getArguments();
      return editorRights.containsKey(args[0]) && editorRights.get(args[0]).equals(args[1]);
    });

    when(datasetService.get(denmarkDataset.getKey())).thenReturn(denmarkDataset);

    when(installationService.get(denmarkInstallation.getKey())).thenReturn(denmarkInstallation);
    when(organizationService.get(denmarkOrganization.getKey())).thenReturn(denmarkOrganization);
  }

  /**
   * The user has permission to edit
   * @throws Exception
   */
  @Test
  public void testGoodCases() throws Exception {
    setRequestUser(datasetEditor);
    mockRequest("DELETE", "dataset/"+ denmarkDataset.getKey().toString());
    filter.filter(mockRequest);

    setRequestUser(installationEditor);
    mockRequest("DELETE", "dataset/"+ denmarkDataset.getKey().toString());
    filter.filter(mockRequest);
    mockRequest("DELETE", "installation/"+ denmarkInstallation.getKey().toString());
    filter.filter(mockRequest);

    setRequestUser(organizationEditor);
    mockRequest("DELETE", "dataset/"+ denmarkDataset.getKey().toString());
    filter.filter(mockRequest);
    mockRequest("DELETE", "installation/"+ denmarkInstallation.getKey().toString());
    filter.filter(mockRequest);
    mockRequest("DELETE", "organization/"+ denmarkOrganization.getKey().toString());
    filter.filter(mockRequest);

    setRequestUser(nodeEditor);
    mockRequest("DELETE", "dataset/"+ denmarkDataset.getKey().toString());
    filter.filter(mockRequest);
    mockRequest("DELETE", "installation/"+ denmarkInstallation.getKey().toString());
    filter.filter(mockRequest);
    mockRequest("DELETE", "organization/"+ denmarkOrganization.getKey().toString());
    filter.filter(mockRequest);
    mockRequest("DELETE", "node/"+ denmarkNode.getKey().toString());
    filter.filter(mockRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testNoRights() throws Exception {
    setRequestUser(nothingEditor);
    mockRequest("DELETE", "dataset/"+ denmarkDataset.getKey().toString());
    filter.filter(mockRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testOnlyDatasetRights() throws Exception {
    setRequestUser(datasetEditor);
    mockRequest("DELETE", "installation/"+ denmarkInstallation.getKey().toString());
    filter.filter(mockRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testOnlyDatasetRights2() throws Exception {
    setRequestUser(datasetEditor);
    mockRequest("DELETE", "organization/"+ denmarkOrganization.getKey().toString());
    filter.filter(mockRequest);
  }

  @Test(expected = WebApplicationException.class)
  public void testOnlyOrganizationRights() throws Exception {
    setRequestUser(organizationEditor);
    mockRequest("DELETE", "node/"+ denmarkNode.getKey().toString());
    filter.filter(mockRequest);
  }

  /* Entity creation */

  @Test
  public void testCreateGoodCases() throws Exception {
    Assert.assertTrue(editorAuthorizationService.allowedToModifyEntity(principal(organizationEditor), denmarkDataset.getPublishingOrganizationKey()));
    Assert.assertTrue(editorAuthorizationService.allowedToModifyEntity(principal(nodeEditor), denmarkOrganization.getEndorsingNodeKey()));
  }

  @Test
  public void testCreateFailCases() throws Exception {
    Assert.assertFalse(editorAuthorizationService.allowedToModifyEntity(principal(nothingEditor), denmarkDataset.getKey()));
    Assert.assertFalse(editorAuthorizationService.allowedToModifyEntity(principal(datasetEditor), denmarkDataset.getPublishingOrganizationKey()));
    Assert.assertFalse(editorAuthorizationService.allowedToModifyEntity(principal(installationEditor), denmarkInstallation.getOrganizationKey()));
    Assert.assertFalse(editorAuthorizationService.allowedToModifyEntity(principal(organizationEditor), denmarkOrganization.getEndorsingNodeKey()));
  }

  private void mockRequest(String method, String path) {
    when(mockRequest.getPath()).thenReturn(path);
    when(mockRequest.getMethod()).thenReturn(method.toUpperCase().trim());
  }

  private void setRequestUser(String user) {
    when(secContext.getUserPrincipal()).thenReturn(principal(user));
  }
  private Principal principal(final String user) {
    return () -> user;
  }
}
