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
