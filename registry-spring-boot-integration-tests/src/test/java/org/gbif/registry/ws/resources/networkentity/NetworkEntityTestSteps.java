package org.gbif.registry.ws.resources.networkentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.gbif.registry.utils.RegistryITUtils;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class, RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class NetworkEntityTestSteps {

  private static final UUID UK_NODE_KEY = UUID.fromString("f698c938-d36a-41ac-8120-c35903e1acb9");
  private static final UUID UK_NODE_2_KEY = UUID.fromString("9996f2f2-f71c-4f40-8e69-031917b314e0");

  private static final Map<String, UUID> NODE_MAP = new HashMap<>();
  private static final Map<String, Class<? extends NetworkEntity>> ENTITIES = new HashMap<>();

  private MockMvc mvc;
  private ResultActions result;
  private NetworkEntity entity;
  private String key;
  private Date modificationDateBeforeUpdate;
  private Date creationDateBeforeUpdate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Before("@NetworkEntity")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_node_prepare.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();

    ENTITIES.put("node", Node.class);
    ENTITIES.put("organization", Organization.class);
    NODE_MAP.put("UK Node", UK_NODE_KEY);
    NODE_MAP.put("UK Node 2", UK_NODE_2_KEY);
  }

  @After("@NetworkEntity")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/organization/organization_cleanup.sql"));
    connection.close();
  }

  @When("create new {word}")
  public void createEntity(String entityType) throws Exception {
    entity = NetworkEntityProvider.prepare(entityType, UK_NODE_KEY, "New entity " + entityType);
    String entityJson = objectMapper.writeValueAsString(entity);

    result = mvc
      .perform(
        post("/" + entityType)
          .with(httpBasic("justadmin", "welcome"))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON))
      .andDo(print());

    key =
      RegistryITUtils.removeQuotes(result.andReturn().getResponse().getContentAsString());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @When("get {word} by key")
  public void getEntityById(String entityType) throws Exception {
    result = mvc
      .perform(
        get("/" + entityType + "/{key}", key));

    entity = objectMapper.readValue(result.andReturn().getResponse().getContentAsByteArray(), ENTITIES.get(entityType));
    // TODO: 16/10/2019 assertLenientEquals and stuff (see registry's NetworkEntityTest)
  }

  @When("update {word} with new title {string}")
  public void updateEntity(String entityType, String newTitle) throws Exception {
    modificationDateBeforeUpdate = entity.getModified();
    creationDateBeforeUpdate = entity.getCreated();
    entity.setTitle(newTitle);

    String entityJson = objectMapper.writeValueAsString(entity);

    result = mvc
      .perform(
        put("/"+ entityType + "/{key}", key)
          .with(httpBasic("justadmin", "welcome"))
          .content(entityJson)
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON));
  }

  @Then("title is new {string}")
  public void checkUpdatedEntityTitle(String newTitle) {
    assertEquals("Entity's title was to be updated", newTitle, entity.getTitle());
  }

  @Then("modification date was updated")
  public void checkModificationDateWasChangedAfterUpdate() {
    assertNotNull(entity.getModified());
    assertTrue("Modification date was to be changed",
      entity.getModified().after(modificationDateBeforeUpdate));
  }

  @Then("modification date is after the creation date")
  public void checkModificationDateIsAfterCreationDate() {
    assertNotNull(entity.getModified());
    assertTrue("Modification date must be after the creation date",
      entity.getModified().after(creationDateBeforeUpdate));
  }
}
