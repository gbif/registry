package org.gbif.registry.ws.guice;

import org.gbif.api.service.checklistbank.NameUsageService;
import org.gbif.checklistbank.ws.client.guice.ChecklistBankWsClientModule;

import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Using the regular clb client module causes an issue with binding named strings several times.
 * even though the module is a private module and one would think these bound names should not be exposed.
 *
 * We therefore create the module in a separate injector and only expose the name usage service here.
 */
public class NameUsageServiceWrapperModule extends AbstractModule {
  private final NameUsageService nameUsageService;

  public NameUsageServiceWrapperModule(Properties p) {
    Module mod = new ChecklistBankWsClientModule(p, true, false);
    Injector inj = Guice.createInjector(mod);
    nameUsageService = inj.getInstance(NameUsageService.class);
  }

  @Override
  protected void configure() {
    bind(NameUsageService.class).toInstance(nameUsageService);
  }
}
