package org.gbif.registry.search;

import org.gbif.api.model.registry.search.DatasetSearchParameter;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;

import java.util.List;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import com.google.common.collect.ImmutableList;

/**
 *
 */
public class SolrMapping {

  public static final String KEY_FIELD = "key";
  public static final BiMap<DatasetSearchParameter, String> FACET_MAPPING = EnumHashBiMap.create(DatasetSearchParameter.class);
  static {
    FACET_MAPPING.put(DatasetSearchParameter.TYPE, "type");
    FACET_MAPPING.put(DatasetSearchParameter.SUBTYPE, "subtype");
    FACET_MAPPING.put(DatasetSearchParameter.COUNTRY, "country");
    FACET_MAPPING.put(DatasetSearchParameter.PUBLISHING_COUNTRY, "publishing_country");
    FACET_MAPPING.put(DatasetSearchParameter.PUBLISHING_ORG, "publishing_organization_key");
    FACET_MAPPING.put(DatasetSearchParameter.HOSTING_ORG, "hosting_organization_key");
    FACET_MAPPING.put(DatasetSearchParameter.DECADE, "decade");
    FACET_MAPPING.put(DatasetSearchParameter.KEYWORD, "keyword");
    FACET_MAPPING.put(DatasetSearchParameter.LICENSE, "license");
    FACET_MAPPING.put(DatasetSearchParameter.PROJECT_ID, "project_id");
    FACET_MAPPING.put(DatasetSearchParameter.TAXON_KEY, "taxon_key");
    FACET_MAPPING.put(DatasetSearchParameter.YEAR, "year");
    FACET_MAPPING.put(DatasetSearchParameter.RECORD_COUNT, "record_count");
  }

  public static final List<String> HIGHLIGHT_FIELDS = ImmutableList.of("description", "vernacular_name");

  /**
   * Converts an internal solr string value into the string representation used in our API.
   */
  public static String interpretSolrValue(DatasetSearchParameter param, String value) {
    if (Strings.isNullOrEmpty(value)) return null;

    if (Enum.class.isAssignableFrom(param.type())) {
      Class<Enum<?>> vocab = (Class<Enum<?>>) param.type();
      Enum<?> enumValue = vocab.getEnumConstants()[Integer.valueOf(value)];
      // If the Enum is either a Country or a Language, its iso2Letter code it's used.
      if (Country.class.equals(param.type())) {
        return ((Country) enumValue).getIso2LetterCode();

      } else if (Language.class.equals(param.type())) {
        return ((Language) enumValue).getIso2LetterCode();

      } else {
        return enumValue.name();
      }

    } else if (UUID.class.isAssignableFrom(param.type())) {
      return UUID.fromString(value).toString();

    } else if (Double.class.isAssignableFrom(param.type())) {
      return String.valueOf(Double.parseDouble(value));

    } else if (Integer.class.isAssignableFrom(param.type())) {
      return String.valueOf(Integer.parseInt(value));

    } else if (Boolean.class.isAssignableFrom(param.type())) {
      return String.valueOf(Boolean.parseBoolean(value));

    }
    return value;
  }

}
