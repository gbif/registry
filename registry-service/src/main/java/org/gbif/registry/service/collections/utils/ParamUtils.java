package org.gbif.registry.service.collections.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Strings;
import org.gbif.registry.persistence.mapper.collections.params.RangeParam;

import java.util.regex.Matcher;

import static org.gbif.registry.service.collections.utils.SearchUtils.INTEGER_RANGE;
import static org.gbif.registry.service.collections.utils.SearchUtils.WILDCARD_SEARCH;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ParamUtils {

  public static RangeParam parseIntegerRangeParameter(String param) {
    if (Strings.isNullOrEmpty(param)) {
      return null;
    }

    RangeParam rangeParam = new RangeParam();
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
  }
}
