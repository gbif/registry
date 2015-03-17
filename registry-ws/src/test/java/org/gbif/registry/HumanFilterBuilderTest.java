package org.gbif.registry;

import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.Country;

import java.util.ResourceBundle;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HumanFilterBuilderTest {
  // make sure this is the right bundle as used by the human query builder
  private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("org/gbif/occurrence/query/filter");

  /**
   * Test all available search parameters and make sure we have a bundle entry for all possible enum values.
   * Test copied from download-query-tools, but added here again as we might have a different GBIF API version in use.
   */
  @Test
  public void testEnumResourceBundle() throws Exception {
    for (OccurrenceSearchParameter p : OccurrenceSearchParameter.values()) {
      if (p.type().isEnum()) {
        if (p.type() != Country.class && p.type() != Continent.class) {
          Class<Enum<?>> vocab = (Class<Enum<?>>) p.type();
          // make sure we have en entry for all possible enum values
          for (Enum<?> e : vocab.getEnumConstants()) {
            assertTrue("Missing enum resource bundle entry for " + vocab.getSimpleName() + "." + e.name(),
              resourceBundle.containsKey("enum." + vocab.getSimpleName().toLowerCase() + "." + e.name()));
          }
        }
      }
    }
  }

}
