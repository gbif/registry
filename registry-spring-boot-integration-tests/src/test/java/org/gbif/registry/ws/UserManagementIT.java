package org.gbif.registry.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.registry.ConfirmationKeyParameter;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.ws.model.UserCreation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Base64Utils;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {TestEmailConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class UserManagementIT {

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private ChallengeCodeMapper challengeCodeMapper;

  private MockMvc mvc;

  @Autowired
  private WebApplicationContext context;

  @Before
  public void setUp() {
    mvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  // TODO: 2019-07-03 rename
  @Test
  public void testCreateUser() throws Exception {
    final UserCreation user = prepareUser();

    final String userJsonString = objectMapper.writeValueAsString(user);

    // perform user creation and check response
    mvc
        .perform(
            post("/admin/user")
                .content(userJsonString)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString("justadmin:welcome".getBytes()))
                .characterEncoding("utf-8"))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("user_13"))
        .andExpect(jsonPath("$.email").value("user_13@gbif.org"))
        .andExpect(jsonPath("$.constraintViolation").doesNotExist())
        .andExpect(jsonPath("$.error").doesNotExist());

    // test we can't login (challengeCode not confirmed)
    // TODO: 2019-07-12 try login -> must fail (implement UserResource first)

    final GbifUser newUser = userMapper.get(user.getUserName());
    final UUID challengeCodeString = challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(newUser.getKey()));
    final ConfirmationKeyParameter confirmation = new ConfirmationKeyParameter(challengeCodeString);
    final String confirmationJsonString = objectMapper.writeValueAsString(confirmation);

    // perform request and check response
    // confirmation user and current one must be the same
    mvc
        .perform(
            post("/admin/user/confirm")
                .content(confirmationJsonString)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64Utils.encodeToString("user_13:welcome".getBytes()))
                .characterEncoding("utf-8"))
        .andDo(print())
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userName").value("user_13"))
        .andExpect(jsonPath("$.email").value("user_13@gbif.org"));

    // test we can login now (challengeCode was confirmed)
    // TODO: 2019-07-12 try login -> must succeed (implement UserResource first)

    // get user ? (challenge code is needed)
    // confirm by challenge code
    // try to update user info
    // change/reset password
    // delete
  }

  private UserCreation prepareUser() {
    UserCreation user = new UserCreation();
    user.setUserName("user_14");
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.setPassword("welcome");
    user.getSettings().put("language", "en");
    user.getSettings().put("country", "dk");
    user.setEmail("user_14@gbif.org");
    return user;
  }
}
