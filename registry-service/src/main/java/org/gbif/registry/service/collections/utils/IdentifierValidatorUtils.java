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

import org.gbif.api.util.validators.identifierschemes.HuhValidator;
import org.gbif.api.util.validators.identifierschemes.IdentifierSchemeValidator;
import org.gbif.api.util.validators.identifierschemes.IhIrnValidator;
import org.gbif.api.util.validators.identifierschemes.IsniValidator;
import org.gbif.api.util.validators.identifierschemes.OrcidValidator;
import org.gbif.api.util.validators.identifierschemes.OtherValidator;
import org.gbif.api.util.validators.identifierschemes.ResearcherIdValidator;
import org.gbif.api.util.validators.identifierschemes.ViafValidator;
import org.gbif.api.util.validators.identifierschemes.WikidataValidator;
import org.gbif.api.vocabulary.collections.IdType;

import java.util.EnumMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IdentifierValidatorUtils {

  private static final HuhValidator HUH_VALIDATOR = new HuhValidator();
  private static final IhIrnValidator IH_IRN_VALIDATOR = new IhIrnValidator();
  private static final IsniValidator ISNI_VALIDATOR = new IsniValidator();
  private static final OrcidValidator ORCID_VALIDATOR = new OrcidValidator();
  private static final ResearcherIdValidator RESEARCHER_ID_VALIDATOR = new ResearcherIdValidator();
  private static final ViafValidator VIAF_VALIDATOR = new ViafValidator();
  private static final WikidataValidator WIKIDATA_VALIDATOR = new WikidataValidator();
  private static final OtherValidator OTHER_VALIDATOR = new OtherValidator();

  private static final Map<IdType, IdentifierSchemeValidator> VALIDATORS_BY_ID_TYPE =
      new EnumMap<>(IdType.class);

  static {
    VALIDATORS_BY_ID_TYPE.put(IdType.HUH, HUH_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.IH_IRN, IH_IRN_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.ISNI, ISNI_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.ORCID, ORCID_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.RESEARCHER_ID, RESEARCHER_ID_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.VIAF, VIAF_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.WIKIDATA, WIKIDATA_VALIDATOR);
    VALIDATORS_BY_ID_TYPE.put(IdType.OTHER, OTHER_VALIDATOR);
  }

  public static IdentifierSchemeValidator getValidatorByIdType(IdType idType) {
    return VALIDATORS_BY_ID_TYPE.get(idType);
  }

  public static IdType getIdType(String userId) {
    if (HUH_VALIDATOR.isValid(userId)) {
      return IdType.HUH;
    }
    if (IH_IRN_VALIDATOR.isValid(userId)) {
      return IdType.IH_IRN;
    }
    if (ISNI_VALIDATOR.isValid(userId)) {
      return IdType.ISNI;
    }
    if (ORCID_VALIDATOR.isValid(userId)) {
      return IdType.ORCID;
    }
    if (RESEARCHER_ID_VALIDATOR.isValid(userId)) {
      return IdType.RESEARCHER_ID;
    }
    if (VIAF_VALIDATOR.isValid(userId)) {
      return IdType.VIAF;
    }
    if (WIKIDATA_VALIDATOR.isValid(userId)) {
      return IdType.WIKIDATA;
    }
    if (OTHER_VALIDATOR.isValid(userId)) {
      return IdType.OTHER;
    }

    throw new IllegalArgumentException("User ID not supported: " + userId);
  }
}
