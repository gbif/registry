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
package org.gbif.registry.ws.it.security;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.database.DatabaseCleaner;
import org.gbif.registry.identity.service.IdentityService;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.security.UserRoles;
import org.gbif.registry.security.precheck.AuthPreCheckInterceptor;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.BiFunction;

import org.hamcrest.text.IsEmptyString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.Base64Utils;

import com.google.common.collect.Sets;

import lombok.SneakyThrows;

import static org.gbif.registry.ws.it.fixtures.TestConstants.TEST_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthPreCheckIT extends BaseItTest {

  private static final String ADMIN = "admin_user";
  private static final String EDITOR = "editor_user";
  private static final String MEDIATOR = "mediator_user";

  private static final BiFunction<String, String, String> BASIC_AUTH_HEADER =
      (username, pass) ->
          "Basic "
              + Base64Utils.encodeToString(
                  (username + ":" + pass).getBytes(StandardCharsets.UTF_8));

  private final MockMvc mockMvc;
  private final IdentityService identityService;
  private final CollectionService collectionService;
  private final DatasetService datasetService;
  private final OrganizationService organizationService;

  private static UUID NODE_KEY;
  private static UUID ORG_KEY;
  private static UUID INSTITUTION_KEY;
  private static UUID INSTITUTION_KEY_2;
  private static final Country COUNTRY = Country.SPAIN;
  private static final String NAMESPACE = "ns";

  @Autowired
  public AuthPreCheckIT(
      MockMvc mockMvc,
      IdentityService identityService,
      CollectionService collectionService,
      DatasetService datasetService,
      OrganizationService organizationService,
      SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.mockMvc = mockMvc;
    this.identityService = identityService;
    this.collectionService = collectionService;
    this.datasetService = datasetService;
    this.organizationService = organizationService;
  }

  @RegisterExtension
  public static DatabaseCleaner databaseCleaner =
      new DatabaseCleaner(PG_CONTAINER, "public.user", "editor_rights", "namespace_rights", "country_rights");

  @SneakyThrows
  @BeforeAll
  public static void loadData(
      @Autowired IdentityService identityService,
      @Autowired NodeService nodeService,
      @Autowired OrganizationService organizationService,
      @Autowired InstitutionService institutionService) {

    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    SecurityContextHolder.setContext(ctx);
    ctx.setAuthentication(
      new UsernamePasswordAuthenticationToken(
        ADMIN,
        "",
        Arrays.asList(
          new SimpleGrantedAuthority(UserRoles.GRSCICOLL_ADMIN_ROLE),
          new SimpleGrantedAuthority(UserRoles.ADMIN_ROLE))));

    GbifUser admin = new GbifUser();
    admin.setUserName(ADMIN);
    admin.setFirstName(ADMIN);
    admin.setLastName(ADMIN);
    admin.setEmail(ADMIN + "@test.com");
    admin.getSettings().put("language", "en");
    admin.getSettings().put("country", "dk");
    admin.setRoles(Sets.newHashSet(UserRole.GRSCICOLL_ADMIN, UserRole.REGISTRY_ADMIN));
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
    identityService.create(editor, TEST_PASSWORD);

    Integer editorKey = identityService.get(EDITOR).getKey();
    identityService.updateLastLogin(editorKey);

    GbifUser mediator = new GbifUser();
    mediator.setUserName(MEDIATOR);
    mediator.setFirstName(MEDIATOR);
    mediator.setLastName(MEDIATOR);
    mediator.setEmail(MEDIATOR + "@test.com");
    mediator.getSettings().put("language", "en");
    mediator.getSettings().put("country", "dk");
    mediator.setRoles(Sets.newHashSet(UserRole.GRSCICOLL_MEDIATOR));
    identityService.create(mediator, TEST_PASSWORD);

    Integer mediatorKey = identityService.get(MEDIATOR).getKey();
    identityService.updateLastLogin(mediatorKey);

    Node node = new Node();
    node.setTitle("title");
    node.setType(NodeType.COUNTRY);
    node.setParticipationStatus(ParticipationStatus.AFFILIATE);
    NODE_KEY = nodeService.create(node);

    Organization organization = new Organization();
    organization.setTitle("title");
    organization.setLanguage(Language.ABKHAZIAN);
    organization.setEndorsingNodeKey(NODE_KEY);
    ORG_KEY = organizationService.create(organization);
    organizationService.confirmEndorsement(ORG_KEY);

    Institution institution = new Institution();
    institution.setCode("i1");
    institution.setName("i1");

    Address address = new Address();
    address.setCountry(COUNTRY);
    institution.setAddress(address);

    INSTITUTION_KEY = institutionService.create(institution);

    Institution institution2 = new Institution();
    institution2.setCode("i2");
    institution2.setName("i2");
    INSTITUTION_KEY_2 = institutionService.create(institution2);
  }

  @Test
  public void creationGrSciCollRequestTest() throws Exception {
    Long collectionsCountBeforeTest =
        collectionService.list(CollectionSearchRequest.builder().build()).getCount();

    resetSecurityContext(ADMIN, UserRole.GRSCICOLL_ADMIN);
    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    resetSecurityContext(EDITOR, UserRole.GRSCICOLL_EDITOR);
    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    identityService.addEditorRight(EDITOR, INSTITUTION_KEY);

    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(IsEmptyString.emptyString()));

    // check that the pre check call didn't perform the action
    assertEquals(
        collectionsCountBeforeTest,
        collectionService.list(CollectionSearchRequest.builder().build()).getCount());

    identityService.deleteEditorRight(EDITOR, INSTITUTION_KEY);

    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    identityService.addCountryRight(EDITOR, COUNTRY);

    mockMvc
        .perform(
            post("/grscicoll/collection")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(IsEmptyString.emptyString()));
  }

  @Test
  public void creationNetworkEntityRequestTest() throws Exception {
    Long datasetsCountBeforeTest = datasetService.list(null).getCount();
    mockMvc
        .perform(
            post("/dataset")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    resetSecurityContext(EDITOR, UserRole.REGISTRY_EDITOR);
    identityService.addEditorRight(EDITOR, NODE_KEY);

    mockMvc
        .perform(
            post("/dataset")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(IsEmptyString.emptyString()));

    // check that the pre check call didn't perform the action
    assertEquals(datasetsCountBeforeTest, datasetService.list(null).getCount());

    // add subresource to the organization
    Long organizationsCountBeforeTest = organizationService.list(null).getCount();
    mockMvc
        .perform(
            post("/organization/" + ORG_KEY + "/identifier")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(IsEmptyString.emptyString()));

    // check that the pre check call didn't perform the action
    assertEquals(organizationsCountBeforeTest, organizationService.list(null).getCount());

    // we remove the editor rights to the node
    identityService.deleteEditorRight(EDITOR, NODE_KEY);
    mockMvc
        .perform(
            post("/organization/" + ORG_KEY + "/identifier")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  public void creationMachineTagRequestTest() throws Exception {
    mockMvc
        .perform(
            post("/node/" + NODE_KEY + "/machineTag")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    resetSecurityContext(EDITOR, UserRole.GRSCICOLL_EDITOR);
    identityService.addNamespaceRight(EDITOR, NAMESPACE);

    mockMvc
        .perform(
            post("/node/" + NODE_KEY + "/machineTag")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(IsEmptyString.emptyString()));
  }

  @Test
  public void creationMergeRequestTest() throws Exception {
    mockMvc
        .perform(
            post("/grscicoll/institution/" + INSTITUTION_KEY + "/merge")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(EDITOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());

    resetSecurityContext(MEDIATOR, UserRole.GRSCICOLL_MEDIATOR);
    identityService.addEditorRight(MEDIATOR, INSTITUTION_KEY);
    identityService.addEditorRight(MEDIATOR, INSTITUTION_KEY_2);

    mockMvc
        .perform(
            post("/grscicoll/institution/" + INSTITUTION_KEY + "/merge")
                .queryParam(AuthPreCheckInterceptor.CHECK_PERMISSIONS_ONLY_PARAM, "true")
                .header("Authorization", BASIC_AUTH_HEADER.apply(MEDIATOR, TEST_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(IsEmptyString.emptyString()));
  }
}
