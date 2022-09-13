package org.gbif.registry.service.collections.utils;

import java.util.regex.Pattern;

public class SearchUtils {

  public static final Pattern NUMBER_SPECIMENS_RANGE =
    Pattern.compile("^(\\d+|\\*)\\s*,\\s*(\\d+|\\*)$");
  public static final String WILDCARD_SEARCH = "*";

}
