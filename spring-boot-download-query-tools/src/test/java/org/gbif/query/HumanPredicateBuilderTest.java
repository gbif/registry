package org.gbif.query;

import org.codehaus.jackson.map.ObjectMapper;
import org.gbif.api.model.occurrence.predicate.InPredicate;
import org.gbif.api.model.occurrence.predicate.Predicate;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HumanPredicateBuilderTest {

  private HumanPredicateBuilder builder;
  private ObjectMapper mapper = new ObjectMapper();

  @Before
  public void init() {
    TitleLookupService tl = mock(TitleLookupService.class);
    when(tl.getDatasetTitle(any())).thenReturn("The little Mermaid");
    when(tl.getSpeciesName(any())).thenReturn("Abies alba Mill.");
    builder = new HumanPredicateBuilder(tl);
  }

  @Test
  public void humanPredicateFilterTest() throws Exception {
    String expected = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader().getResource("result.txt").getPath())));
    try (Stream<String> stream = Files.lines(Paths.get(getClass().getClassLoader().getResource("source.txt").getPath()))) {
      String actual = stream
          .map(p -> p.substring(1, p.length() - 1))
          .map(stringPredicate -> {
            try {
              mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(stringPredicate));
              return builder.humanFilterString(stringPredicate);
            } catch (Exception e) {
              System.out.println("there is an exception");
              return null;
            }
          }).collect(Collectors.joining("\n---------------------------------------------\n"));

      assertEquals(expected, actual);
    }
  }

  @Test
  public void testTooManyLookups() throws Exception {
    // If there are more than 10,050 lookups (dataset, taxa) give up; it's likely to be too slow.

    List<String> bigList = new ArrayList<>();
    for (int i = 0; i < 11000; i++) {
      bigList.add("" + i);
    }
    Predicate bigIn = new InPredicate(OccurrenceSearchParameter.TAXON_KEY, bigList);

    try {
      builder.humanFilter(bigIn);
      fail();
    } catch (IllegalStateException e) {
    }

    try {
      builder.humanFilterString(bigIn);
      fail();
    } catch (IllegalStateException e) {
    }
  }
}
