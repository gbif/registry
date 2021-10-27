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
package org.gbif.registry.search.dataset.indexing;

import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;

import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;

/**
 * A utility to extract decades (eg 1980, 1840) or centuries (eg 1400, 1500, 1600) from
 * TemporalCoverages as a list of integers. Max/min bounderies for supplied values can be specific
 * in the constructor for both a decade and a century range to avoid large list of decades for very
 * old or future periods, mostly for bad data.
 */
public class TimeSeriesExtractor {
  private static final Logger LOG = LoggerFactory.getLogger(TimeSeriesExtractor.class);

  private final int minCentury;
  private final int maxCentury;
  private final int minDecade;
  private final int maxDecade;
  private final Range<Integer> decadeRange;

  /**
   * @param minCentury the minimum value ever extracted, eg 1500
   * @param maxCentury the maximum value ever extracted, eg 2400
   * @param minDecade the lowest decade value to be extracted, needs to be within the century range.
   *     Eg 1870
   * @param maxDecade the largest decade value to be extracted, needs to be within the century
   *     range. Eg 2020
   */
  public TimeSeriesExtractor(int minCentury, int maxCentury, int minDecade, int maxDecade) {
    Preconditions.checkArgument(
        minDecade <= maxDecade, "MinDecade must be below or equals maxDecade");
    Preconditions.checkArgument(
        minCentury <= minDecade, "Century limits must be wider than decade boundaries");
    Preconditions.checkArgument(
        maxCentury >= maxDecade, "Century limits must be wider than decade boundaries");
    Preconditions.checkArgument(minCentury % 100 == 0, "minCentury needs to be a multiple of 100");
    Preconditions.checkArgument(maxCentury % 100 == 0, "maxCentury needs to be a multiple of 100");
    Preconditions.checkArgument(minDecade % 10 == 0, "minDecade needs to be a multiple of 10");
    Preconditions.checkArgument(maxDecade % 10 == 0, "maxDecade needs to be a multiple of 10");
    this.minDecade = minDecade;
    this.maxDecade = maxDecade;
    this.minCentury = minCentury;
    this.maxCentury = maxCentury;
    this.decadeRange = Range.closed(minDecade, maxDecade);
  }

  private Set<Integer> decadesFromInt(int start, int end) {
    Set<Integer> decades = Sets.newHashSet();

    if (start > end) {
      LOG.warn("Potentially inverted year range: {} - {}", start, end);
      return decades;
    }

    Range<Integer> range = Range.closed(start, end);
    // produce centuries only if outside of decade range
    if (!decadeRange.encloses(range)) {
      // skip anything below min/max
      int startC = 100 * (int) Math.floor(minmax(minCentury, maxCentury, start) / 100d);
      int endC = 100 * (int) Math.floor(minmax(minCentury, maxCentury, end) / 100d);
      for (int year = startC; year <= endC; year += 100) {
        decades.add(year);
      }
    }

    // Produce decades if falling within the decade range
    if (decadeRange.isConnected(range)) {
      int startD = 10 * (int) Math.floor(minmax(minDecade, maxDecade, start) / 10d);
      int endD = 10 * (int) Math.floor(minmax(minDecade, maxDecade, end) / 10d);
      for (int year = startD; year <= endD; year += 10) {
        decades.add(year);
      }
    }

    return decades;
  }

  private int minmax(int min, int max, int val) {
    return val < min ? min : (val > max ? max : val);
  }

  /**
   * Produces a list of 4 digit decades or centuries with no duplicates, following normal ordering.
   * TODO: handle VerbatimTimePeriod?
   *
   * @param temporalCoverages the various time periods
   * @return a list of 4 digit decades with no duplicates, ordered numerically
   */
  public List<Integer> extractDecades(List<TemporalCoverage> temporalCoverages) {
    SortedSet<Integer> decades = new TreeSet<Integer>();
    if (temporalCoverages != null && !temporalCoverages.isEmpty()) {
      for (TemporalCoverage tc : temporalCoverages) {
        if (tc instanceof DateRange) {
          DateRange dr = (DateRange) tc;
          Calendar cal = Calendar.getInstance();
          if (dr.getStart() != null && dr.getEnd() != null) {
            cal.setTime(dr.getStart());
            int start = cal.get(Calendar.YEAR);
            cal.setTime(dr.getEnd());
            int end = cal.get(Calendar.YEAR);
            decades.addAll(decadesFromInt(start, end));
          }
        } else if (tc instanceof SingleDate) {
          SingleDate sd = (SingleDate) tc;
          if (sd.getDate() != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sd.getDate());
            int year = cal.get(Calendar.YEAR);
            decades.addAll(decadesFromInt(year, year));
          }
        }
      }
    }
    return Lists.newArrayList(decades);
  }
}
