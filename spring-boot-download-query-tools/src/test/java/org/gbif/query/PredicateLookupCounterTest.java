package org.gbif.query;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.gbif.api.model.occurrence.predicate.ConjunctionPredicate;
import org.gbif.api.model.occurrence.predicate.DisjunctionPredicate;
import org.gbif.api.model.occurrence.predicate.EqualsPredicate;
import org.gbif.api.model.occurrence.predicate.InPredicate;
import org.gbif.api.model.occurrence.predicate.Predicate;
import org.gbif.api.model.occurrence.predicate.WithinPredicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.Country;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class PredicateLookupCounterTest {
  private PredicateLookupCounter counter = new PredicateLookupCounter();

  /**
   * test all available search parameters and make sure we have a bundle entry for all possible enum values
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testAllParams() {

    final String date = DateFormatUtils.ISO_8601_EXTENDED_DATE_FORMAT.format(new Date());
    List<Predicate> ands = Lists.newArrayList();
    for (OccurrenceSearchParameter p : OccurrenceSearchParameter.values()) {
      if (p.type().isEnum()) {
        if (p.type() == Country.class) {
          ands.add(new EqualsPredicate(p, Country.DENMARK.getIso2LetterCode()));

        } else if (p.type() == Continent.class) {
          ands.add(new EqualsPredicate(p, Continent.AFRICA.getTitle()));

        } else {
          Class<Enum<?>> vocab = (Class<Enum<?>>) p.type();
          // add a comparison for every possible enum value to test the resource bundle for completeness
          List<Predicate> ors = Lists.newArrayList();
          for (Enum<?> e : vocab.getEnumConstants()) {
            ors.add(new EqualsPredicate(p, e.toString()));
          }
          ands.add(new DisjunctionPredicate(ors));
        }

      } else if (p.type() == Date.class) {
        ands.add(new EqualsPredicate(p, date));

      } else if (p.type() == Double.class) {
        ands.add(new EqualsPredicate(p, "12.478"));

      } else if (p.type() == Integer.class) {
        ands.add(new EqualsPredicate(p, "10"));

      } else if (p.type() == String.class) {
        if (p == OccurrenceSearchParameter.GEOMETRY) {
          ands.add(new WithinPredicate("POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))"));
        } else {
          ands.add(new EqualsPredicate(p, "Bernd Neumann"));
        }

      } else if (p.type() == Boolean.class) {
        ands.add(new EqualsPredicate(p, "true"));

      } else if (p.type() == UUID.class) {
        ands.add(new EqualsPredicate(p, UUID.randomUUID().toString()));

      } else {
        throw new IllegalStateException("Unknown SearchParameter type " + p.type());
      }
    }
    ConjunctionPredicate and = new ConjunctionPredicate(ands);

    int c = counter.count(and);
    assertEquals(12, c);
  }

  @Test
  public void testTaxa() {
    int c = counter.count(new InPredicate(OccurrenceSearchParameter.TAXON_KEY, Lists.newArrayList("1", "2", "3")));

    assertEquals(3, c);
  }

  @Test
  public void testCountNull() {
    assertEquals(0, (int) counter.count(null));
  }
}
