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
package org.gbif.registry.ws.converter;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import org.springframework.core.convert.converter.Converter;

public class CountryMessageConverter implements Converter<String, Country> {

  @Override
  public Country convert(String source) {
    return parseCountry(source);
  }

  private Country parseCountry(String param) {
    Country country = Country.fromIsoCode(param);
    if (country == null) {
      // if nothing found also try by enum name
      country = VocabularyUtils.lookupEnum(param, Country.class);
    }
    return country;
  }
}
