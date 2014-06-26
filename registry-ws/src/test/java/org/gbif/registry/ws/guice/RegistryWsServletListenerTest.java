package org.gbif.registry.ws.guice;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.common.UserService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.security.GbifAppAuthService;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Injector;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class RegistryWsServletListenerTest {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(RegistryTestModules.database());

  private static Properties properties;

  static {
    try {
      properties = PropertiesUtil.loadProperties("registry-test.properties");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Makes sure that two mybatis projects each with its own Datasource work fine together.
   * Tests the complete listener module, calling real methods to force guice to finalize the bindings.
   */
  @Test
  public void testListenerModule() {
    RegistryWsServletListener mod = new RegistryWsServletListener(properties);
    Injector injector = mod.getInjector();

    GbifAppAuthService auth = injector.getInstance(GbifAppAuthService.class);
    assertNotNull(auth);

    DatasetService datasetService = injector.getInstance(DatasetService.class);
    datasetService.list(new PagingRequest());

    UserService userService = injector.getInstance(UserService.class);
    userService.get("admin");

    OrganizationService orgService = injector.getInstance(OrganizationService.class);
    orgService.list(new PagingRequest());
  }

}
