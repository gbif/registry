package org.gbif.registry.persistence.mapper.collections.params;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.collections.CollectionFacetParameter;

@Getter
@SuperBuilder
public class DescriptorsListParams extends CollectionListParams {

  // descriptors fields
  List<String> usageName;
  List<String> usageKey;
  List<String> usageRank;
  List<String> taxonKey;
  @Nullable List<Country> descriptorCountry;
  @Nullable List<RangeParam<Integer>> individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable List<RangeParam<LocalDate>> dateIdentified;
  @Nullable List<String> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> issues;

  // facets
  @Nullable CollectionFacetParameter facet;

  public boolean descriptorFacet() {
    return facet == CollectionFacetParameter.DESCRIPTOR_COUNTRY
        || facet == CollectionFacetParameter.KINGDOM_KEY
        || facet == CollectionFacetParameter.PHYLUM_KEY
        || facet == CollectionFacetParameter.CLASS_KEY
        || facet == CollectionFacetParameter.ORDER_KEY
        || facet == CollectionFacetParameter.FAMILY_KEY
        || facet == CollectionFacetParameter.GENUS_KEY
        || facet == CollectionFacetParameter.SPECIES_KEY
        || facet == CollectionFacetParameter.TYPE_STATUS
        || facet == CollectionFacetParameter.RECORDED_BY
        || facet == CollectionFacetParameter.OBJECT_CLASSIFICATION
        || facet == CollectionFacetParameter.TAXON_KEY;
  }

  public boolean descriptorSearch() {
    return query != null
        || usageName != null
        || usageKey != null
        || usageRank != null
        || taxonKey != null
        || descriptorCountry != null
        || individualCount != null
        || identifiedBy != null
        || dateIdentified != null
        || typeStatus != null
        || recordedBy != null
        || discipline != null
        || objectClassification != null
        || issues != null;
  }

  public boolean descriptorSearchWithoutQuery() {
    return usageName != null
        || usageKey != null
        || usageRank != null
        || taxonKey != null
        || descriptorCountry != null
        || individualCount != null
        || identifiedBy != null
        || dateIdentified != null
        || typeStatus != null
        || recordedBy != null
        || discipline != null
        || objectClassification != null
        || issues != null;
  }

  public boolean isArrayFieldFacet() {
    return facet == CollectionFacetParameter.PRESERVATION_TYPE
        || facet == CollectionFacetParameter.CONTENT_TYPE
        || facet == CollectionFacetParameter.TYPE_STATUS
        || facet == CollectionFacetParameter.RECORDED_BY
        || facet == CollectionFacetParameter.TAXON_KEY;
  }
}
