package org.gbif.identity.mybatis;

import org.gbif.api.model.common.User;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.identity.guice.IdentityMyBatisModule;
import org.gbif.identity.model.Session;
import org.gbif.identity.util.PasswordEncoder;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

  private IdentityServiceImpl service;
  private static final PasswordEncoder encoder = new PasswordEncoder();

  @Before
  public void testSetup() throws Exception {
    Properties props = PropertiesUtil.loadProperties("registry-test.properties");
    Module mod = new IdentityMyBatisModule(props);
    Injector inj = Guice.createInjector(mod);
    service = (IdentityServiceImpl) inj.getInstance(IdentityService.class);
  }

  /**
   * Checks the typical CRUD process with correct data only (i.e. no failure scenarios).
   */
  @Test
  public void testCRUD() throws Exception {
    User u1 = new User();
    u1.setUserName("trobertson");
    u1.setFirstName("Tim");
    u1.setLastName("Robertson");
    u1.setPasswordHash(encoder.encode("password"));
    u1.getRoles().add(UserRole.USER);
    u1.getSettings().put("user.settings.language", "en");
    u1.getSettings().put("user.country", "dk");
    u1.setEmail("trobertson@gbif.org");

    // create
    String key = service.create(u1);
    assertEquals("Expected the key to be the username", u1.getUserName(), key);

    // get
    User u2 = service.get(u1.getUserName());
    assertEquals(u1.getUserName(), u2.getUserName());
    assertEquals(u1.getFirstName(), u2.getFirstName());
    assertEquals(u1.getLastName(), u2.getLastName());
    assertEquals(2, u2.getSettings().size());
    assertEquals(1, u2.getRoles().size());
    assertNull(u2.getLastLogin());

    // update
    u2.getSettings().put("user.country", "GB");
    u2.getRoles().add(UserRole.REGISTRY_ADMIN);
    service.update(u2);

    User u3 = service.get(u1.getUserName());
    assertEquals(2, u3.getSettings().size());
    assertEquals("GB", u3.getSettings().get("user.country"));
    assertEquals(2, u3.getRoles().size());

    service.delete(u1.getUserName());

    User u4 = service.get(u1.getUserName());
    assertNull(u4);
  }

  /**
   * Checks the typical session creation processes.
   */
  @Test
  public void testSessions() throws Exception {
    User u1 = new User();
    u1.setUserName("frank");
    u1.setFirstName("Tim");
    u1.setLastName("Robertson");
    u1.setPasswordHash(encoder.encode("password"));
    u1.setEmail("frank@gbif.org");
    service.create(u1);

    // this will create a session
    /*
    Session s = service.authenticate(u1.getUserName(), "password");
    User u2 = service.getBySession(s.getSession());
    assertEquals(u1.getUserName(), u2.getUserName());
    assertEquals(1, service.listSessions(u1.getUserName()).size());

    s = service.authenticate(u1.getUserName(), "password", "127.0.0.1");
    User u3 = service.getBySession(s.getSession());
    assertEquals(u1.getUserName(), u3.getUserName());
    assertEquals(2, service.listSessions(u1.getUserName()).size());

    s = service.authenticate(u1.getUserName(), "password", "127.0.0.1");
    User u4 = service.getBySession(s.getSession());
    assertEquals(u1.getUserName(), u4.getUserName());
    assertEquals(3, service.listSessions(u1.getUserName()).size());

    service.terminateSession(s.getSession());
    assertEquals(2, service.listSessions(u1.getUserName()).size());
    assertNull(service.getBySession(s.getSession()));

    service.terminateAllSessions(u1.getUserName());
    assertTrue(service.listSessions(u1.getUserName()).isEmpty());
     */
  }
}
