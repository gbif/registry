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

import org.gbif.api.model.collections.IdType;
import org.gbif.api.util.validators.identifierschemes.HuhValidator;
import org.gbif.api.util.validators.identifierschemes.IdentifierSchemeValidator;
import org.gbif.api.util.validators.identifierschemes.IhIrnValidator;
import org.gbif.api.util.validators.identifierschemes.IsniValidator;
import org.gbif.api.util.validators.identifierschemes.OrcidValidator;
import org.gbif.api.util.validators.identifierschemes.OtherValidator;
import org.gbif.api.util.validators.identifierschemes.ResearcherIdValidator;
import org.gbif.api.util.validators.identifierschemes.ViafValidator;
import org.gbif.api.util.validators.identifierschemes.WikidataValidator;

import java.util.EnumMap;
import java.util.Map;

public class IdentifierValidatorFactory {

  private static final Map<IdType, IdentifierSchemeValidator> VALIDATORS_BY_ID_TYPE =
      new EnumMap<>(IdType.class);

  static {
    VALIDATORS_BY_ID_TYPE.put(IdType.HUH, new HuhValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.IH_IRN, new IhIrnValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.ISNI, new IsniValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.ORCID, new OrcidValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.RESEARCHER_ID, new ResearcherIdValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.VIAF, new ViafValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.WIKIDATA, new WikidataValidator());
    VALIDATORS_BY_ID_TYPE.put(IdType.OTHER, new OtherValidator());
  }

  public static IdentifierSchemeValidator getValidatorByIdType(IdType idType) {
    return VALIDATORS_BY_ID_TYPE.get(idType);
  }
}
