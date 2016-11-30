package org.gbif.registry.search.util;

import org.gbif.registry.search.DatasetIndexUpdateListener;
import org.gbif.registry.search.SolrInitializer;
import org.gbif.registry.search.guice.RegistrySearchModule;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.gbif.registry.guice.RegistryTestModules.webservice;

/**
 *
 */
public class DatasetIndexBuilderTest {
  static Injector inj;
  static DatasetIndexBuilder idxBuilder;

  // Resets SOLR between each method
  @Rule
  public SolrInitializer solrRule;

  @BeforeClass
  public static void init() {
    inj = webservice();
    idxBuilder = inj.getInstance(DatasetIndexBuilder.class);
  }

  @Before
  public void initTest() {
    solrRule = new SolrInitializer(
        inj.getInstance(RegistrySearchModule.DATASET_KEY),
        inj.getInstance(DatasetIndexUpdateListener.class)
    );
  }

  @Test
  public void build() throws Exception {
    idxBuilder.build();
  }

}