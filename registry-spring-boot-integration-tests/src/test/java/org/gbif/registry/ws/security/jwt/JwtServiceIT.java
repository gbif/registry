package org.gbif.registry.ws.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.model.collections.Person;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.TestEmailConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: 2019-07-15 mb rename to JwtIT because it's testing not only JwtService but also all jwt functionality and stuff
@SpringBootTest(classes = {TestEmailConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class JwtServiceIT {

  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private WebApplicationContext context;

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // TODO: 2019-07-15 rename
  @Test
  public void validTokenTest() throws Exception {
    final String token = login();

    mvc
        .perform(
            post("/grscicoll/person")
                .content(asJsonString(createPerson()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
//                .characterEncoding("utf-8")
                )
        .andDo(print())
        .andExpect(status().isCreated());
// TODO: 2019-07-25 assert token is not null and doesn't match previous one
  }

  // TODO: 2019-07-15 rename
  @Test
  public void invalidHeaderTest() throws Exception {
    final String token = login();

    SecurityContextHolder.clearContext();

    mvc
        .perform(
            post("/grscicoll/person")
                .header(HttpHeaders.AUTHORIZATION, "beare " + token)
                .content(asJsonString(createPerson()))
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8"))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  // TODO: 2019-07-15 rename
  @Test
  public void invalidTokenTest() {

  }

  // TODO: 2019-07-15 rename
  @Test
  public void insufficientRolesTest() {

  }

  // TODO: 2019-07-15 rename
  @Test
  public void fakeUserTest() {

  }

  // TODO: 2019-07-15 rename
  @Test
  public void noJwtAndNoBasicAuthTest() {

  }

  // TODO: 2019-07-15 rename
  @Test
  public void noJwtWithBasicAuthTest() {

  }

  public String asJsonString(final Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Person createPerson() {
    Person newPerson = new Person();
    newPerson.setFirstName("first name");
    newPerson.setCreatedBy("Test");
    newPerson.setModifiedBy("Test");
    return newPerson;
  }

  private String login() throws Exception {
    // login first (see UserManagementIT)
    ResultActions resultActions = mvc
        .perform(
            post("/user/login")
                .with(httpBasic("justadmin", "welcome"))
                .characterEncoding("utf-8"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());

    final MvcResult result = resultActions.andReturn();
    final String contentAsString = result.getResponse().getContentAsString();

    final LoggedUserWithToken loggedUserWithToken = objectMapper.readValue(contentAsString, LoggedUserWithToken.class);
    return loggedUserWithToken.getToken();
  }

}
