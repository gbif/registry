package org.gbif.registry.ws.resources.pipelines;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.registry.RegistryIntegrationTestsConfiguration;
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
import java.util.Map;
import java.util.Objects;

import static org.gbif.registry.utils.matcher.RegistryMatchers.isRegistryLocalDateTimeFormat;
import static org.gbif.registry.ws.fixtures.TestConstants.TEST_PASSWORD;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {RegistryIntegrationTestsConfiguration.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PipelinesTestSteps {

  private ResultActions result;
  private MockMvc mvc;
  private int executionKey;
  private int stepKey;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private DataSource ds;

  private Connection connection;

  @Before("@Pipelines")
  public void setUp() throws Exception {
    connection = ds.getConnection();
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/pipelines/pipelines_cleanup.sql"));
    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/pipelines/pipelines_prepare.sql"));

    mvc = MockMvcBuilders
      .webAppContextSetup(context)
      .apply(springSecurity())
      .build();
  }

  @After("@Pipelines")
  public void tearDown() throws Exception {
    Objects.requireNonNull(connection, "Connection must not be null");

    ScriptUtils.executeSqlScript(connection,
      new ClassPathResource("/scripts/pipelines/pipelines_cleanup.sql"));

    connection.close();
  }

  @Given("datasets")
  public void prepareDatasets(DataTable dataTable) {
    // prepared by scripts in @Before
  }

  @Given("pipeline processes")
  public void preparePipelineProcesses(DataTable dataTable) {
    // prepared by scripts in @Before
  }

  @Given("pipeline execution")
  public void preparePipelineExecution(DataTable dataTable) {
    // prepared by scripts in @Before
  }

  @Given("pipeline step")
  public void preparePipelineStep(DataTable dataTable) {
    // prepared by scripts in @Before
  }

  @Given("pipeline execution with key {int}")
  public void setExecutionKey(int executionKey) {
    this.executionKey = executionKey;
  }

  @When("create pipeline process using {word} {string} with params")
  public void createPipelineProcess(String userType, String username, PipelineProcessParameters parameters) throws Exception {
    String content = objectMapper.writeValueAsString(parameters);

    result = mvc
      .perform(
        post("/pipelines/history/process/")
          .with(httpBasic(username, TEST_PASSWORD))
          .content(content)
          .contentType(MediaType.APPLICATION_JSON)
      );
  }

  @When("get pipeline process by datasetKey {string} and attempt {int}")
  public void getPipelineProcess(String datasetKey, int attempt) throws Exception {
    result = mvc
      .perform(
        get("/pipelines/history/{datasetKey}/{attempt}", datasetKey, attempt)
      )
      .andDo(print())
    ;
  }

  @When("history pipeline process")
  public void historyPipelineProcess() throws Exception {
    result = mvc
      .perform(
        get("/pipelines/history")
      )
      .andDo(print())
    ;
  }

  @When("history pipeline process by datasetKey {string}")
  public void historyPipelineProcess(String datasetKey) throws Exception {
    result = mvc
      .perform(
        get("/pipelines/history/{datasetKey}", datasetKey)
      )
      .andDo(print())
    ;
  }

  @When("add pipeline execution for process {int} using {word} {string}")
  public void addPipelineExecution(int processKey, String userType, String username, PipelineExecution pipelineExecution) throws Exception {
    String content = objectMapper.writeValueAsString(pipelineExecution);

    result = mvc
      .perform(
        post("/pipelines/history/process/{processKey}", processKey)
          .with(httpBasic(username, TEST_PASSWORD))
          .content(content)
          .contentType(MediaType.APPLICATION_JSON)
      )
      .andDo(print())
    ;
  }

  @When("add pipeline step for process {int} and current execution using {word} {string}")
  public void addPipelineStep(int processKey, String userType, String username, PipelineStep pipelineStep) throws Exception {
    String content = objectMapper.writeValueAsString(pipelineStep);

    result = mvc
      .perform(
        post("/pipelines/history/process/{processKey}/{executionKey}", processKey, executionKey)
          .with(httpBasic(username, TEST_PASSWORD))
          .content(content)
          .contentType(MediaType.APPLICATION_JSON)
      )
      .andDo(print())
    ;
  }

  @When("get pipeline step by stepKey for process {int} and current execution")
  public void historyPipelineProcess(int processKey) throws Exception {
    result = mvc
      .perform(
        get("/pipelines/history/process/{processKey}/{executionKey}/{stepKey}", processKey, executionKey, stepKey)
      )
      .andDo(print())
    ;
  }

  @Then("response status should be {int}")
  public void assertResponseCode(int status) throws Exception {
    result
      .andExpect(status().is(status));
  }

  @Then("pipeline process is")
  public void assertPipelineProcessResponse(Map<String, String> expectedData) throws Exception {
    result.andExpect(jsonPath("$.created").value(isRegistryLocalDateTimeFormat()));
    for (Map.Entry<String, String> entry : expectedData.entrySet()) {
      result.andExpect(jsonPath("$." + entry.getKey()).value(entry.getValue()));
    }
  }

  @Then("pipeline process is empty")
  public void assertPipelineProcessEmpty() throws Exception {
    result.andExpect(content().string(""));
  }

  @Then("pipeline process history contains {int} entity")
  public void assertHistory(int count) throws Exception {
    result
      .andExpect(jsonPath("$.count").value(count))
      .andExpect(jsonPath("$.endOfRecords").value(true));
  }

  @Then("extract executionKey")
  public void extractExecutionKey() throws Exception {
    String contentAsString = result.andReturn().getResponse().getContentAsString();
    executionKey = Integer.parseInt(contentAsString);
  }

  @Then("extract stepKey")
  public void extractStepKey() throws Exception {
    String contentAsString = result.andReturn().getResponse().getContentAsString();
    stepKey = Integer.parseInt(contentAsString);
  }

  @Then("pipeline step is")
  public void assertPipelineStepResponse(Map<String, String> expectedData) throws Exception {
    result.andExpect(jsonPath("$.started").value(isRegistryLocalDateTimeFormat()));
    for (Map.Entry<String, String> entry : expectedData.entrySet()) {
      result.andExpect(jsonPath("$." + entry.getKey()).value(entry.getValue()));
    }
  }
}
