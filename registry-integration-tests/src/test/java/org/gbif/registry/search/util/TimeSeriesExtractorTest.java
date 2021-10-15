/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.search.util;

import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.registry.search.dataset.indexing.TimeSeriesExtractor;

import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    List<Integer> expected =
        Lists.newArrayList(1000, 1910, 1970, 1980, 1990, 2200, 2300, 2400, 2500);
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
