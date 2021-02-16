package org.gbif.registry.ws.it.security;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.security.precheck.AuthPreCheckInterceptor;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;

import com.google.common.collect.Sets;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_ADMIN;
import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthPreCheckIT extends BaseItTest {

  private static final String ADMIN = "admin_user";
  private static final String EDITOR = "editor_user";

  private static final BiFunction<String, String, String> BASIC_AUTH_HEADER =
      (username, pass) ->
          "Basic "
              + Base64Utils.encodeToString(
                  (username + ":" + pass).getBytes(StandardCharsets.UTF_8));

  private final MockMvc mockMvc;
  private final IdentityService identityService;
  private final InstitutionService institutionService;
  private final NodeService nodeService;
  private final OrganizationService organizationService;
  private final DatasetService datasetService;

  @Autowired
  public AuthPreCheckIT(
      MockMvc mockMvc,
      IdentityService identityService,
      InstitutionService institutionService,
      NodeService nodeService,
      OrganizationService organizationService,
      DatasetService datasetService,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.mockMvc = mockMvc;
    this.identityService = identityService;
    this.institutionService = institutionService;
    this.nodeService = nodeService;
    this.organizationService = organizationService;
    this.datasetService = datasetService;
  }

  @BeforeEach
  public void initUsers() {
    GbifUser admin = new GbifUser();
    admin.setUserName(ADMIN);
    admin.setFirstName(ADMIN);
    admin.setLastName(ADMIN);
    admin.setEmail(ADMIN + "@test.com");
    admin.getSettings().put("language", "en");
    admin.getSettings().put("country", "dk");
    admin.setRoles(Sets.newHashSet(UserRole.GRSCICOLL_ADMIN, UserRole.REGISTRY_ADMIN));

    // password equals to username
    identityService.create(admin, TEST_PASSWORD);

    Integer adminKey = identityService.get(ADMIN).getKey();
    identityService.updateLastLogin(adminKey);

    GbifUser editor = new GbifUser();
    editor.setUserName(EDITOR);
    editor.setFirstName(EDITOR);
    editor.setLastName(EDITOR);
    editor.setEmail(EDITOR + "@test.com");
    editor.getSettings().put("language", "en");
    editor.getSettings().put("country", "dk");
    editor.setRoles(Sets.newHashSet(UserRole.GRSCICOLL_EDITOR, UserRole.REGISTRY_EDITOR));

    // password equals to username
    identityService.create(editor, TEST_PASSWORD);

    Integer editorKey = identityService.get(EDITOR).getKey();
    identityService.updateLastLogin(editorKey);
  }

  @Test
  public void creationGrSciCollRequestTest() throws Exception {
    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    resetSecurityContext(TEST_ADMIN, UserRole.GRSCICOLL_ADMIN);
    Institution institution = new Institution();
    institution.setCode("i1");
    institution.setName("i1");
    UUID institutionKey = institutionService.create(institution);
    identityService.addEditorRight(EDITOR, institutionKey);

    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // check that the pre check call didn't perform the action
    assertEquals(1, institutionService.list(InstitutionSearchRequest.builder().build()).getCount());
  }

  @Test
  public void creationNetworkEntityRequestTest() throws Exception {
    mockMvc
        .perform(
            post("/dataset")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    resetSecurityContext(TEST_ADMIN, UserRole.REGISTRY_ADMIN);
    Node node = new Node();
    node.setTitle("title");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    UUID nodeKey = nodeService.create(node);
    identityService.addEditorRight(EDITOR, nodeKey);

    Organization organization = new Organization();
    organization.setTitle("title");
    organization.setLanguage(Language.ABKHAZIAN);
    organization.setEndorsingNodeKey(nodeKey);
    UUID orgKey = organizationService.create(organization);
    organizationService.confirmEndorsement(orgKey);

    mockMvc
        .perform(
            post("/dataset")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // check that the pre check call didn't perform the action
    assertEquals(0, datasetService.list(null).getCount());

    // add subresource to the organization
    mockMvc
        .perform(
            post("/organization/" + orgKey + "/identifier")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    // we remove the editor rights to the node
    identityService.deleteEditorRight(EDITOR, nodeKey);
    mockMvc
        .perform(
            post("/organization/" + orgKey + "/identifier")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }
}
