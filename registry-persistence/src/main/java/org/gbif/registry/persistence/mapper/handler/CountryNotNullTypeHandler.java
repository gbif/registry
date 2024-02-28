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
package org.gbif.registry.persistence.mapper.handler;

import org.gbif.api.vocabulary.Country;
import org.gbif.mybatis.type.BaseEnumTypeHandler;
import org.gbif.mybatis.type.EnumConverter;

public class CountryNotNullTypeHandler extends BaseEnumTypeHandler<String, Country> {
  public CountryNotNullTypeHandler() {
    super(new CountryNotNullConverter());
  }

  public static class CountryNotNullConverter implements EnumConverter<String, Country> {
    public CountryNotNullConverter() {}

    @Override
    public String fromEnum(Country value) {
      return value != null ? value.getIso2LetterCode() : null;
    }

    @Override
    public Country toEnum(String key) {
      if (key == null) {
        return null;
      } else {
        Country c = Country.fromIsoCode(key);
        return c == null ? Country.UNKNOWN : c;
      }
    }
  }
}
