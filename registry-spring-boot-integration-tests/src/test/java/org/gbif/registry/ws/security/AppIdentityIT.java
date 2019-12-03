package org.gbif.registry.ws.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.gbif.ws.util.SecurityConstants.GBIF_SCHEME_PREFIX;
import static org.gbif.ws.util.SecurityConstants.HEADER_GBIF_USER;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class AppIdentityIT {

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

  // TODO: 2019-08-12 remove?
  @Test
  public void performTestWithValidTokenShouldReturnStatusCreatedAndUpdateToken() throws Exception {
    mvc
        .perform(
            post("/test/app")
                .header(HEADER_GBIF_USER, "gbif.registry-ws-client-it")
                .header(HttpHeaders.AUTHORIZATION, GBIF_SCHEME_PREFIX + "gbif.registry-ws-client-it:ag5xzHiB2yCFNJalo+2W0j7QnK0="))
        .andExpect(status().isCreated())
        .andReturn();
  }
}
