package org.gbif.registry.doi;

import org.gbif.api.model.common.DOI;
import org.gbif.registry.database.DatabaseInitializer;
import org.gbif.registry.guice.RegistryTestModules;

import java.util.Set;

import com.beust.jcommander.internal.Sets;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DoiGeneratorMQIT {
  private DoiGenerator generator;

  @Rule
  public final DatabaseInitializer databaseRule = new DatabaseInitializer(RegistryTestModules.database());

  @Before
  public void setup() {
    Injector inj = RegistryTestModules.webservice();
    generator = inj.getInstance(DoiGenerator.class);
  }

  @Test
  public void testNewDOI() {
    Set<DOI> dois = Sets.newHashSet();
    for (int x = 1; x < 20; x++) {
      DOI doi = generator.newDatasetDOI();
      assertTrue(generator.isGbif(doi));
      dois.add(doi);
      assertEquals(DOI.TEST_PREFIX, doi.getPrefix());
      assertEquals(x, dois.size());
    }
  }

}