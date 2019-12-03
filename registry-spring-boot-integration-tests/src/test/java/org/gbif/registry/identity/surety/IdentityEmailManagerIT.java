package org.gbif.registry.identity.surety;

import org.gbif.api.model.ChallengeCode;
import org.gbif.api.model.common.GbifUser;
import org.gbif.registry.domain.mail.BaseEmailModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * Tests related to {@link IdentityEmailManager}.
 * The main purpose of the following tests is to ensure we can generate a {@link BaseEmailModel} using the
 * Freemarker templates.
 */
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class IdentityEmailManagerIT {

  @Autowired
  private IdentityEmailManager identityEmailManager;

  private GbifUser generateTestUser() {
    GbifUser newUser = new GbifUser();
    newUser.setUserName("User");
    newUser.setEmail("a@b.com");
    return newUser;
  }

  @Test
  public void testGenerateNewUserEmailModel() throws IOException {
    GbifUser newUser = generateTestUser();
    BaseEmailModel baseEmail =
      identityEmailManager.generateNewUserEmailModel(newUser, ChallengeCode.newRandom());
    assertNotNull("We can generate the model from the template", baseEmail);
  }

  @Test
  public void testGenerateResetPasswordEmailModel() throws IOException {
    GbifUser newUser = generateTestUser();
    BaseEmailModel baseEmail =
      identityEmailManager.generateResetPasswordEmailModel(newUser, ChallengeCode.newRandom());
    assertNotNull("We can generate the model from the template", baseEmail);
  }

  @Test
  public void testGenerateWelcomeEmailModel() throws IOException {
    GbifUser newUser = new GbifUser();
    newUser.setUserName("User");
    newUser.setEmail("a@b.com");
    BaseEmailModel baseEmail =
      identityEmailManager.generateWelcomeEmailModel(newUser);
    assertNotNull("We can generate the model from the template", baseEmail);
  }

}
