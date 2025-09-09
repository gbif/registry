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

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.License;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public abstract class BaseSearchDto {

  private UUID key;
  private String code;
  private String name;
  private String description;
  private boolean active;
  private Country country;
  private Country mailingCountry;
  private String city;
  private String mailingCity;
  private boolean displayOnNHCPortal;
  private List<AlternativeCode> alternativeCodes = new ArrayList<>();
  private URI featuredImageUrl;
  private License featuredImageLicense;
  private String featuredImageAttribution;

  // highlights
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
  private String descriptorUsageNameHighlight;
  private String descriptorCountryHighlight;
  private String descriptorIdentifiedByHighlight;
  private String descriptorTypeStatusHighlight;
  private String descriptorRecordedByHighlight;
  private String descriptorDisciplineHighlight;
  private String descriptorObjectClassificationHighlight;
  private String descriptorBiomeHighlight;
  private String descriptorIssuesHighlight;
  private String descriptorGroupTitleHighlight;
  private String descriptorGroupDescriptionHighlight;
  private boolean similarityMatch;
}
