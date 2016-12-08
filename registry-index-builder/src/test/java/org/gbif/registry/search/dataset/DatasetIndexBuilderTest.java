package org.gbif.registry.search.dataset;

import org.gbif.api.service.registry.DatasetService;
import org.gbif.registry.directory.DirectoryModule;
import org.gbif.registry.events.EventModule;
import org.gbif.registry.persistence.guice.RegistryMyBatisModule;
import org.gbif.registry.search.DatasetIndexService;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.utils.file.properties.PropertiesUtil;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;

/**
 *
 */
public class DatasetIndexBuilderTest {

  @Test
  public void build() throws Exception {
      // read properties and check args
      Properties props = PropertiesUtil.loadProperties("registry-test.properties");
      Injector inj = Guice.createInjector(
          new RegistryMyBatisModule(props),
          new RegistrySearchModule(props),
          new DirectoryModule(props),
          new StubModule(),
          EventModule.withoutRabbit(props)
      );

      DatasetIndexBuilder idxBuilder = new DatasetIndexBuilder(inj.getInstance(DatasetService.class), inj.getInstance(DatasetIndexService.class));
      idxBuilder.build();
  }

}