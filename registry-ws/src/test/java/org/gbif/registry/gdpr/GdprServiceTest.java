package org.gbif.registry.gdpr;

import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.database.LiquibaseModules;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Injector;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.gbif.common.messaging.api.messages.GdprNotificationMessage.EntityType;
import static org.gbif.registry.guice.RegistryTestModules.webservice;

/**
 * Tests the {@link GdprService}.
 */
public class GdprServiceTest {

  /**
   * Truncates the tables
   */
  @Rule
  public final DatabaseInitializer initializer = new DatabaseInitializer(LiquibaseModules.database());

  private static final String TEST_EMAIL = "test@test.com";

  private static Injector injector;
  private static Map<EntityType, List<UUID>> context;
  private static GdprService gdprService;

  @BeforeClass
  public static void setup() {
    injector = webservice();
    gdprService = injector.getInstance(GdprService.class);

    // create context
    context = new HashMap<>();
    context.put(EntityType.Dataset,
                Arrays.asList(UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c21"),
                              UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c22")));
    context.put(EntityType.Node, Arrays.asList(UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c23")));
    context.put(EntityType.Network, Arrays.asList(UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c24")));
    context.put(EntityType.Installation, Arrays.asList(UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c25")));
    context.put(EntityType.Organization,
                Arrays.asList(UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c26"),
                              UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c27"),
                              UUID.fromString("75956ee6-1a2b-4fa3-b3e8-ccda64ce6c28")));
  }

  @Test
  public void existsNotificationTest() {
    GdprConfiguration config = injector.getInstance(GdprConfiguration.class);

    gdprService.createNotification(TEST_EMAIL, null, context);

    Assert.assertTrue(gdprService.existsNotification(TEST_EMAIL));
    Assert.assertTrue(gdprService.existsNotification(TEST_EMAIL, config.getVersion()));
    Assert.assertFalse(gdprService.existsNotification(TEST_EMAIL + "aa"));
    Assert.assertFalse(gdprService.existsNotification(TEST_EMAIL, "-1"));
  }

  @Test(expected = PersistenceException.class)
  public void duplicateKeyTest() {
    gdprService.createNotification(TEST_EMAIL, null, context);
    gdprService.createNotification(TEST_EMAIL, null, context);
  }

}
