package org.gbif.registry.ws.guice;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.service.common.IdentityService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.database.LiquibaseInitializer;
import org.gbif.registry.database.LiquibaseModules;
import org.gbif.registry.ws.surety.OrganizationSuretyModule;
import org.gbif.utils.file.properties.PropertiesUtil;
import org.gbif.ws.security.GbifAuthService;

import java.io.IOException;
import java.util.Properties;

import com.google.inject.Injector;
import com.google.inject.Key;
import org.junit.ClassRule;
import org.junit.Test;

import static org.gbif.registry.ws.surety.OrganizationSuretyModule.ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF;

import static org.junit.Assert.assertNotNull;

/**
 * Tests to make sure we can instantiate {@link RegistryWsServletListener}.
 * This also make sure Guice bindings are working.
 */
public class RegistryWsServletListenerTest {

  // Flushes the database on each run
  @ClassRule
  public static final LiquibaseInitializer liquibaseRule = new LiquibaseInitializer(LiquibaseModules.database());

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
   * Tests the complete listener module, calling real methods to force Guice to finalize the bindings.
   */
  @Test
  public void testListenerModule() {
    RegistryWsServletListener mod = new RegistryWsServletListener(properties);
    Injector injector = mod.getInjector();

    GbifAuthService auth = injector.getInstance(GbifAuthService.class);
    assertNotNull(auth);

    DatasetService datasetService = injector.getInstance(DatasetService.class);
    datasetService.list(new PagingRequest());

    IdentityService identityService = injector.getInstance(IdentityService.class);
    identityService.get("admin");

    OrganizationService orgService = injector.getInstance(OrganizationService.class);
    orgService.list(new PagingRequest());
  }

  /**
   * The module {@link OrganizationSuretyModule} contains conditional binding on organization.surety.mail.enable configuration.
   * The purpose of this test is to ensure the Guice bindings are working if we set this configuration to 'true'.
   */
  @Test
  public void testConditionalModule() {
    Properties props = new Properties(properties);
    props.setProperty("organization.surety.mail.enable", "true");

    RegistryWsServletListener mod = new RegistryWsServletListener(properties);
    Injector injector = mod.getInjector();

    injector.getInstance(Key.get(ORGANIZATION_ENDORSEMENT_SERVICE_TYPE_REF));
  }

}
