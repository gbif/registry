package org.gbif.identity.service;

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.inject.IdentityTestModule;
import org.gbif.identity.model.ModelMutationError;
import org.gbif.identity.model.UserModelMutationResult;
import org.gbif.identity.mybatis.DatabaseInitializer;
import org.gbif.identity.mybatis.IdentitySuretyTestHelper;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.surety.InMemoryEmailManager;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class IdentityServiceImplIT {

  // Sets up the database schema
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

  // truncates the tables
  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(LiquibaseModules.database());

  private static final String TEST_PASSWORD = "[password]";
  private static final String TEST_PASSWORD2 = "]password[";
  private static final AtomicInteger index = new AtomicInteger(0);

  private IdentityService identityService;

  private IdentitySuretyTestHelper identitySuretyTestHelper;
  private InMemoryEmailManager inMemoryEmailManager;

  @Before
  public void testSetup() throws Exception {
    Properties props = PropertiesUtil.loadProperties("registry-test.properties");

    Module mod = new IdentityTestModule(props);
    Injector inj = Guice.createInjector(mod);
    identityService = inj.getInstance(IdentityService.class);
    identitySuretyTestHelper = inj.getInstance(IdentitySuretyTestHelper.class);

    //get the concrete type of the EmailSender
    inMemoryEmailManager = inj.getInstance(InMemoryEmailManager.class);
  }

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCRUD() throws Exception {
    GbifUser u1 = generateUser();

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    // get
    GbifUser u2 = identityService.get(u1.getUserName());
    assertEquals(u1.getUserName(), u2.getUserName());
    assertEquals(u1.getFirstName(), u2.getFirstName());
    assertEquals(u1.getLastName(), u2.getLastName());
    assertEquals(2, u2.getSettings().size());
    assertEquals(1, u2.getRoles().size());
    assertNull(u2.getLastLogin());

    // update
    u2.getSettings().put("user.country", "GB");
    u2.getSystemSettings().put("internal.settings", "-7");

    UserModelMutationResult mutationResult = identityService.update(u2);
    assertNotNull("got mutationResult", mutationResult);
    assertFalse("Doesn't contain error like " + mutationResult.getConstraintViolation(), mutationResult.containsError());

    GbifUser u3 = identityService.get(u1.getUserName());
    assertEquals(2, u3.getSettings().size());
    assertEquals("GB", u3.getSettings().get("user.country"));
    assertEquals("-7", u3.getSystemSettings().get("internal.settings"));

    identityService.delete(u1.getKey());
    GbifUser u4 = identityService.get(u1.getUserName());
    assertNull(u4);
  }

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCreateError() throws Exception {
    GbifUser u1 = generateUser();
    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    // try to create it again with a different username (but same email)
    u1.setKey(null); //reset key
    u1.setUserName("user_x");
    result = identityService.create(u1, TEST_PASSWORD);
    assertEquals("Expected USER_ALREADY_EXIST (user already exists)", ModelMutationError.USER_ALREADY_EXIST, result.getError());

    u1.setUserName("");
    u1.setEmail("email@email.com");
    result = identityService.create(u1, TEST_PASSWORD);
    assertEquals("Expected CONSTRAINT_VIOLATION for empty username", ModelMutationError.CONSTRAINT_VIOLATION, result.getError());

    // try with a password too short
    u1.setUserName("user_x");
    result = identityService.create(u1, "p");
    assertEquals("Expected PASSWORD_LENGTH_VIOLATION", ModelMutationError.PASSWORD_LENGTH_VIOLATION, result.getError());
  }

  /**
   * Checks that the get(username) is case insensitive.
   */
  @Test
  public void testGetIsCaseInsensitive() throws Exception {
    GbifUser u1 = generateUser();
    u1.setUserName("testuser");
    u1.setEmail("myEmail@b.com");

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set. " + result.getConstraintViolation(), result.getUsername());

    GbifUser newUser = identityService.get("tEstuSeR");
    assertNotNull("Can get the user using the same username with capital letters", newUser.getKey());
    //ensure we stored the email by respecting the case
    assertEquals("myEmail@b.com", newUser.getEmail());

    //but we should be able to login using the lowercase version
    newUser = identityService.get("myemail@b.com");
    assertNotNull("Can get the user using the email in lowercase", newUser.getKey());
  }

  @Test
  public void testCreateUserChallengeCodeSequence() {
    GbifUser user = createConfirmedUser(identityService, identitySuretyTestHelper, inMemoryEmailManager);
    assertNotNull(user);
  }

  @Test
  public void testResetPasswordSequence() {
    GbifUser user = createConfirmedUser(identityService, identitySuretyTestHelper, inMemoryEmailManager);
    identityService.resetPassword(user.getKey());

    //ensure we can not login
    assertNull("Can not login until the password is changed", identityService.authenticate(user.getUserName(), TEST_PASSWORD));

    //confirm challenge code
    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(user.getKey());
    assertNotNull("Got a challenge code for " + user.getEmail(), challengeCode);
    assertTrue("password can be changed using challengeCode",
            !identityService.updatePassword(user.getKey(), TEST_PASSWORD2, challengeCode).containsError());

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", identityService.authenticate(user.getUserName(), TEST_PASSWORD2));
  }

  /**
   * Generates a different user on each call.
   * Thread-Safe
   * @return
   */
  public static GbifUser generateUser() {
    int idx = index.incrementAndGet();
    GbifUser user = new GbifUser();
    user.setUserName("user_" + idx);
    user.setFirstName("Tim");
    user.setLastName("Robertson");
    user.getRoles().add(UserRole.USER);
    user.getSettings().put("user.settings.language", "en");
    user.getSettings().put("user.country", "dk");
    user.getSystemSettings().put("internal.settings", "18");
    user.setEmail("user_" + idx + "@gbif.org");
    return user;
  }

  /**
   * Creates a new user and confirms its challenge code.
   * No assertion performed.
   * @return
   */
  public static GbifUser createConfirmedUser(IdentityService identityService, IdentitySuretyTestHelper identitySuretyTestHelper,
                                         InMemoryEmailManager inMemoryEmailManager) {
    GbifUser u1 = generateUser();
    // create the user
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    //ensure we got an email
    assertNotNull("The user got an email with the challenge code", inMemoryEmailManager.getEmail(u1.getEmail()));

    //ensure we can not login
    assertNull("Can not login until the challenge code is confirmed", identityService.authenticate(u1.getUserName(), TEST_PASSWORD));

    UUID challengeCode = identitySuretyTestHelper.getChallengeCode(u1.getKey());
    //confirm challenge code
    assertNotNull("Got a challenge code for email: " + u1.getEmail(), challengeCode);

    GbifUser user = identityService.get(u1.getUserName());
    assertTrue("challengeCode can be confirmed", identityService.confirmUser(u1.getKey(), challengeCode));

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", identityService.authenticate(u1.getUserName(), TEST_PASSWORD));
    return user;
  }
}
