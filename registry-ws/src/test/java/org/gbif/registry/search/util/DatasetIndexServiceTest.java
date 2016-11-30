package org.gbif.registry.search.util;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.License;
import org.gbif.registry.search.DatasetIndexService;
import org.gbif.registry.search.DatasetIndexUpdateListener;
import org.gbif.registry.search.SolrInitializer;
import org.gbif.registry.search.guice.RegistrySearchModule;
import org.gbif.utils.text.StringUtils;

import java.util.Random;
import java.util.UUID;

import com.google.inject.Injector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.gbif.registry.guice.RegistryTestModules.webservice;

/**
 *
 */
public class DatasetIndexServiceTest {
  static Injector inj;
  static DatasetIndexService service;

  // Resets SOLR between each method
  @Rule
  public SolrInitializer solrRule;

  @BeforeClass
  public static void init() {
    inj = webservice();
    service = inj.getInstance(DatasetIndexService.class);
  }

  @Before
  public void initTest() {
    solrRule = new SolrInitializer(
        inj.getInstance(RegistrySearchModule.DATASET_KEY),
        inj.getInstance(DatasetIndexUpdateListener.class)
    );
  }

  @Test
  public void add() throws Exception {
    Random rnd = new Random();
    for (int i=1; i<100; i++) {
      Dataset d = new Dataset();
      d.setKey(UUID.randomUUID());
      d.setType(DatasetType.values()[rnd.nextInt(DatasetType.values().length)]);
      d.setTitle("Title "+i);
      d.setDescription(StringUtils.randomString(50*i));
      d.setLicense(License.values()[rnd.nextInt(License.values().length)]);
      service.add(d);
    }
  }

  @Test
  public void delete() throws Exception {

  }

}