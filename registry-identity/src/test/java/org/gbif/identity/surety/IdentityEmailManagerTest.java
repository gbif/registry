package org.gbif.identity.surety;

import org.gbif.api.model.common.GbifUser;
import org.gbif.identity.IdentityConstants;
import org.gbif.registry.surety.SuretyConstants;
import org.gbif.registry.surety.email.BaseEmailModel;
import org.gbif.registry.surety.model.ChallengeCode;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ibatis.io.Resources;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Unit tests related to {@link IdentityEmailManager}.
 */
public class IdentityEmailManagerTest {
  public static final String APPLICATION_PROPERTIES = "registry-test.properties";

  private IdentityEmailManager identityEmailManager;

  @Before
  public void setup() throws IOException {
    final Properties p = new Properties();
    p.load(Resources.getResourceAsStream(APPLICATION_PROPERTIES));
    Properties filteredProperties = PropertiesUtil.filterProperties(p, IdentityConstants.PROPERTY_PREFIX + SuretyConstants.PROPERTY_PREFIX);
    Injector inj = Guice.createInjector(new InternalIdentitySuretyModule(filteredProperties));
    identityEmailManager = inj.getInstance(IdentityEmailManager.class);
  }

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
