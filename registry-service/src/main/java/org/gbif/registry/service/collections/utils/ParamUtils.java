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
package org.gbif.registry.service.collections.utils;

import org.gbif.api.model.collections.request.SearchRequest;
import org.gbif.api.util.IsoDateParsingUtils;
import org.gbif.api.util.Range;
import org.gbif.api.util.SearchTypeValidator;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.collections.params.RangeParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.service.collections.utils.SearchUtils.INTEGER_RANGE;
import static org.gbif.registry.service.collections.utils.SearchUtils.WILDCARD_SEARCH;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParamUtils {

  public static List<RangeParam<Integer>> parseIntegerRangeParameters(List<String> params) {
    if (params == null || params.isEmpty()) {
      return null;
    }

    return params.stream()
        .map(
            param -> {
              RangeParam<Integer> rangeParam = new RangeParam<>();
              Matcher matcher = INTEGER_RANGE.matcher(param);
              if (matcher.matches()) {
                String lowerString = matcher.group(1);
                if (!lowerString.equals(WILDCARD_SEARCH)) {
                  rangeParam.setLowerBound(Integer.valueOf(lowerString));
                }

                String higherString = matcher.group(2);
                if (!higherString.equals(WILDCARD_SEARCH)) {
                  rangeParam.setHigherBound(Integer.valueOf(higherString));
                }
              } else {
                try {
                  rangeParam.setExactValue(Integer.valueOf(param));
                } catch (Exception ex) {
                  log.info("Invalid range {}", param, ex);
                }
              }
              return rangeParam;
            })
        .filter(rp -> !rp.isEmpty())
        .collect(Collectors.toList());
  }

  public static List<RangeParam<LocalDate>> parseDateRangeParameters(List<String> params) {
    if (params == null || params.isEmpty()) {
      return null;
    }

    return params.stream()
        .map(
            param -> {
              RangeParam<LocalDate> rangeParam = new RangeParam<>();

              if (SearchTypeValidator.isDateRange(param)) {
                Range<LocalDate> range = IsoDateParsingUtils.parseDateRange(param);
                return new RangeParam<>(range.lowerEndpoint(), range.upperEndpoint(), null);
              } else {
                return new RangeParam<>(null, null, IsoDateParsingUtils.parseDate(param));
              }
            })
        .filter(rp -> !rp.isEmpty())
        .collect(Collectors.toList());
  }

  public static List<Country> parseGbifRegion(SearchRequest searchRequest) {
    List<Country> countries = new ArrayList<>();
    if (searchRequest.getGbifRegion() != null && !searchRequest.getGbifRegion().isEmpty()) {
      countries.addAll(
          Arrays.stream(Country.values())
              .filter(c -> searchRequest.getGbifRegion().contains(c.getGbifRegion()))
              .collect(Collectors.toList()));
    }
    return countries;
  }
}
