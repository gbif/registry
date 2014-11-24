package org.gbif.registry.search.util;

import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;

import java.util.Calendar;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeSeriesExtractorTest {

  @Test
  public void testExtractDecades() throws Exception {

    TimeSeriesExtractor ex = new TimeSeriesExtractor(1000, 2500, 1800, 2050);

    List<TemporalCoverage> coverages = Lists.newArrayList();
    coverages.add(range(1977, 1995));
    coverages.add(range(1914, 1917));
    coverages.add(range(987, 999));
    coverages.add(range(2222, 3333));

    List<Integer> periods = ex.extractDecades(coverages);
    List<Integer> expected = Lists.newArrayList(1000, 1910, 1970, 1980, 1990, 2200, 2300, 2400, 2500);
    assertEquals(expected, periods);
  }

  @Test
  public void testSwappedDecades() throws Exception {

    TimeSeriesExtractor ex = new TimeSeriesExtractor(1000, 2500, 1800, 2050);

    List<TemporalCoverage> coverages = Lists.newArrayList();
    coverages.add(range(1995, 1977));
    coverages.add(range(1914, 1917));
    coverages.add(range(987, 999));
    coverages.add(range(3333, 2222));

    List<Integer> periods = ex.extractDecades(coverages);
    List<Integer> expected = Lists.newArrayList(1000, 1910);
    assertEquals(expected, periods);
  }

  private DateRange range(int year1, int year2) {
    DateRange dr = new DateRange();

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year1);
    dr.setStart(cal.getTime());

    cal.set(Calendar.YEAR, year2);
    dr.setEnd(cal.getTime());

    return dr;
  }
}
