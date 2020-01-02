package org.gbif.registry.ws.resources.collections.institution;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class InstitutionTestSteps {

  private ResultActions result;
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private DataSource ds;
  private Connection connection;

  @Before("@Institution")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/institution/institution_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/institution/institution_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@Institution")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/institution/institution_cleanup.sql"));

    connection.close();
  }

  @When("call suggest institutions with query {string} by {word}")
  public void suggestInstitutions(String query, String type) throws Exception {
    result = mvc
      .perform(
        get("/grscicoll/institution/suggest")
          .param("q", query)
      )
      .andDo(print());
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("{int} institution\\(s) should be suggested")
  public void assertInstitutionsSuggested(int suggestedNumber) throws Exception {
    result
      .andExpect(jsonPath("$").isArray())
      .andExpect(jsonPath("$.length()").value(suggestedNumber));
  }
}
