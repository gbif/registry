package org.gbif.registry.identity.service;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.identity.model.ModelMutationError;
import org.gbif.registry.identity.model.UserModelMutationResult;
import org.gbif.registry.identity.surety.IdentityEmailManager;
import org.gbif.registry.persistence.mapper.ChallengeCodeMapper;
import org.gbif.registry.persistence.mapper.UserMapper;
import org.gbif.registry.surety.ChallengeCodeManager;
import org.gbif.registry.surety.email.EmailSender;
import org.gbif.registry.surety.email.InMemoryEmailSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

// TODO: 2019-07-02 add appKeyWhiteList
@SpringBootTest
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
public class IdentityServiceImplIT {

  private static final String TEST_PASSWORD = "[password]";
  private static final String TEST_PASSWORD2 = "]password[";
  private static final AtomicInteger index = new AtomicInteger(0);

  @Autowired
  private DataSource dataSource;

  @Autowired
  @Qualifier("inMemoryEmailSender")
  private EmailSender inMemoryEmailSender;

  @Autowired
  private ChallengeCodeMapper challengeCodeMapper;

  @Autowired
  private UserMapper userMapper;

  @Autowired
  private ChallengeCodeManager<Integer> challengeCodeManager;

  @Autowired
  private IdentityEmailManager identityEmailManager;

  private IdentityService identityService;

  // TODO: 2019-07-02 replace with configuration or stuff
  @Before
  public void setUp() {
    UserSuretyDelegate userSuretyDelegate = new UserSuretyDelegateImpl(inMemoryEmailSender, challengeCodeManager, identityEmailManager);
    identityService = new IdentityServiceImpl(userMapper, userSuretyDelegate);

    cleanDb();
  }

  // TODO: 2019-07-02 before each run?
  private void cleanDb() {
      try (Connection connection = dataSource.getConnection();
           PreparedStatement deleteUsers = connection.prepareStatement("TRUNCATE public.user");
           PreparedStatement deleteRight = connection.prepareStatement("TRUNCATE editor_rights")) {
        deleteUsers.execute();
        deleteRight.execute();
      } catch (SQLException e) {
        Throwables.propagate(e);
      }
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
  public void testGetBySystemSetting() throws Exception {
    GbifUser u1 = generateUser();
    u1.setSystemSettings(ImmutableMap.of("my.app.setting", "secret-magic"));

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set. " + result.getConstraintViolation(), result.getUsername());
    GbifUser newUser = identityService.getBySystemSetting("my.app.setting", "secret-magic");
    assertNotNull("Can get the user using systemSettings", newUser.getKey());

    newUser = identityService.getBySystemSetting("my.app.setting", "wrong-magic");
    assertNull("Can NOT get the user using wrong systemSettings", newUser);
  }

  // TODO: 2019-07-02 floating behaviour (fails if run all tests)
  @Test
  public void testCreateUserChallengeCodeSequence() {
    GbifUser user = createConfirmedUser(identityService, inMemoryEmailSender);
    assertNotNull(user);
  }

  @Test
  public void testResetPasswordSequence() {
    GbifUser user = createConfirmedUser(identityService, inMemoryEmailSender);
    identityService.resetPassword(user.getKey());

    //ensure we can not login
    assertNull("Can not login until the password is changed", identityService.authenticate(user.getUserName(), TEST_PASSWORD));

    //confirm challenge code
    UUID challengeCode = getChallengeCode(user.getKey());
    assertNotNull("Got a challenge code for " + user.getEmail(), challengeCode);
    assertTrue("password can be changed using challengeCode",
        !identityService.updatePassword(user.getKey(), TEST_PASSWORD2, challengeCode).containsError());

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", identityService.authenticate(user.getUserName(), TEST_PASSWORD2));
  }

  // TODO: 2019-07-02 floating behaviour
  @Test
  public void testCrudEditorRights() {
    GbifUser u1 = generateUser();

    // create
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    UUID randomUuid = UUID.randomUUID();

    identityService.addEditorRight(result.getUsername(), randomUuid);

    assertEquals(1, identityService.listEditorRights(result.getUsername()).size());
    assertEquals(randomUuid, identityService.listEditorRights(result.getUsername()).get(0));

    identityService.deleteEditorRight(result.getUsername(), randomUuid);

    assertEquals(0, identityService.listEditorRights(result.getUsername()).size());
  }

  /**
   * Generates a different user on each call.
   * Thread-Safe
   *
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
   *
   * @return
   */
  public GbifUser createConfirmedUser(IdentityService identityService, EmailSender inMemoryEmailManager) {
    GbifUser u1 = generateUser();
    // create the user
    UserModelMutationResult result = identityService.create(u1, TEST_PASSWORD);
    assertNotNull("Expected the Username to be set", result.getUsername());

    //ensure we got an email
    assertNotNull("The user got an email with the challenge code", ((InMemoryEmailSender) inMemoryEmailManager).getEmail(u1.getEmail()));

    //ensure we can not login
    assertNull("Can not login until the challenge code is confirmed", identityService.authenticate(u1.getUserName(), TEST_PASSWORD));

    UUID challengeCode = getChallengeCode(u1.getKey());
    //confirm challenge code
    assertNotNull("Got a challenge code for email: " + u1.getEmail(), challengeCode);

    GbifUser user = identityService.get(u1.getUserName());
    assertTrue("challengeCode can be confirmed", identityService.confirmUser(u1.getKey(), challengeCode));

    //ensure we can now login
    assertNotNull("Can login after the challenge code is confirmed", identityService.authenticate(u1.getUserName(), TEST_PASSWORD));
    return user;
  }

  private UUID getChallengeCode(Integer entityKey) {
    return challengeCodeMapper.getChallengeCode(userMapper.getChallengeCodeKey(entityKey));
  }
}
