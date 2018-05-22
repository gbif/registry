package org.gbif.registry.dataprivacy.email;

import org.gbif.registry.surety.email.BaseEmailModel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Injector;
import freemarker.template.TemplateException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.gbif.common.messaging.api.messages.DataPrivacyNotificationMessage.EntityType;
import static org.gbif.registry.guice.RegistryTestModules.webservice;

/**
 * Tests the {@link DataPrivacyEmailManager}.
 */
public class DataPrivacyEmailManagerTest {

  private static final String TEST_EMAIL = "test@test.com";

  private static Injector injector;
  private static Map<EntityType, List<UUID>> context;
  private static DataPrivacyEmailManager emailManager;

  @BeforeClass
  public static void setup() {
    injector = webservice();
    emailManager = injector.getInstance(DataPrivacyEmailManager.class);

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
  public void sampleUrlsTest() {
    List<URL> urls = new ArrayList<>();
    try {
      urls = emailManager.generateSampleUrls(context);
    } catch (MalformedURLException e) {
      Assert.fail(e.getMessage());
    }

    Assert.assertEquals(context.values().stream().mapToInt(Collection::size).sum(), urls.size());
  }

  @Test
  public void testGenerateEmail() {
    DataPrivacyEmailConfiguration config = injector.getInstance(DataPrivacyEmailConfiguration.class);

    BaseEmailModel baseEmailModel = null;
    try {
      baseEmailModel = emailManager.generateDataPrivacyNotificationEmail(TEST_EMAIL, context);
    } catch (IOException | TemplateException e) {
      Assert.fail(e.getMessage());
    }

    Assert.assertNotNull(baseEmailModel);
    Assert.assertEquals(TEST_EMAIL, baseEmailModel.getEmailAddress());
    Assert.assertEquals(config.getSubject(), baseEmailModel.getSubject());
    Assert.assertNull(baseEmailModel.getCcAddress());
    Assert.assertNotNull(baseEmailModel.getBody());
  }

  @Test
  public void sendEmailTest() {
    Assert.assertTrue(emailManager.sendDataPrivacyNotification(TEST_EMAIL, context));
  }

}
