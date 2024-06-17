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
package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.vocabulary.Country;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchDto {

  private float score;
  private String type;
  private UUID key;
  private String code;
  private String name;
  private UUID institutionKey;
  private String institutionCode;
  private String institutionName;
  private boolean displayOnNHCPortal;
  private Country country;
  private Country mailCountry;

  private String codeHighlight;
  private String nameHighlight;
  private String descriptionHighlight;
  private String alternativeCodesHighlight;
  private String addressHighlight;
  private String cityHighlight;
  private String provinceHighlight;
  private String countryHighlight;
  private String mailAddressHighlight;
  private String mailCityHighlight;
  private String mailProvinceHighlight;
  private String mailCountryHighlight;

  private boolean similarityMatch;
}
