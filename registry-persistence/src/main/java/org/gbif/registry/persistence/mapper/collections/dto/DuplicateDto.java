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

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicateDto {

  private UUID key1;
  private String code1;
  private String name1;
  private Country physicalCountry1;
  private String physicalCity1;
  private Country mailingCountry1;
  private String mailingCity1;
  private LocalDateTime created1;
  private LocalDateTime modified1;
  private UUID key2;
  private String code2;
  private String name2;
  private Country physicalCountry2;
  private String physicalCity2;
  private Country mailingCountry2;
  private String mailingCity2;
  private LocalDateTime created2;
  private LocalDateTime modified2;
  private LocalDateTime generatedDate;
  private boolean codeMatch;
  private boolean nameMatch;
  private boolean cityMatch;
  private boolean countryMatch;
  private boolean fuzzyNameMatch;

  // only for collections
  private UUID institutionKey1;
  private UUID institutionKey2;
  private boolean institutionKeyMatch;

  @Override
  public String toString() {
    return new StringJoiner(", ", DuplicateDto.class.getSimpleName() + "[", "]")
        .add("key1=" + key1)
        .add("code1='" + code1 + "'")
        .add("name1='" + name1 + "'")
        .add("physicalCountry1=" + physicalCountry1)
        .add("physicalCity1='" + physicalCity1 + "'")
        .add("mailingCountry1=" + mailingCountry1)
        .add("mailingCity1='" + mailingCity1 + "'")
        .add("created1=" + created1)
        .add("modified1=" + modified1)
        .add("key2=" + key2)
        .add("code2='" + code2 + "'")
        .add("name2='" + name2 + "'")
        .add("physicalCountry2=" + physicalCountry2)
        .add("physicalCity2='" + physicalCity2 + "'")
        .add("mailingCountry2=" + mailingCountry2)
        .add("mailingCity2='" + mailingCity2 + "'")
        .add("created2=" + created2)
        .add("modified2=" + modified2)
        .add("generatedDate=" + generatedDate)
        .add("codeMatch=" + codeMatch)
        .add("nameMatch=" + nameMatch)
        .add("cityMatch=" + cityMatch)
        .add("countryMatch=" + countryMatch)
        .add("fuzzyNameMatch=" + fuzzyNameMatch)
        .add("institutionKey1=" + institutionKey1)
        .add("institutionKey2=" + institutionKey2)
        .add("institutionKeyMatch=" + institutionKeyMatch)
        .toString();
  }
}
