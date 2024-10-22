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

import java.util.regex.Pattern;

public class SearchUtils {

  public static final Pattern INTEGER_RANGE =
      Pattern.compile("^(\\d+|\\*)\\s*,\\s*(\\d+|\\*)$");
  public static final String WILDCARD_SEARCH = "*";
  public static final int DEFAULT_FACET_LIMIT = 10;
}
