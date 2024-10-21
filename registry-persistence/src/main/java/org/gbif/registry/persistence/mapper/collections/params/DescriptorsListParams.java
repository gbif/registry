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
  @Nullable List<String> usageName;
  @Nullable List<Integer> usageKey;
  @Nullable List<Rank> usageRank;
  @Nullable List<Integer> taxonKey;
  @Nullable List<Country> descriptorCountry;
  @Nullable RangeParam individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable LocalDate dateIdentifiedFrom;
  @Nullable LocalDate dateIdentifiedBefore;
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
        || facet == CollectionFacetParameter.SPECIES_KEY;
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
        || dateIdentifiedFrom != null
        || dateIdentifiedBefore != null
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
        || dateIdentifiedFrom != null
        || dateIdentifiedBefore != null
        || typeStatus != null
        || recordedBy != null
        || discipline != null
        || objectClassification != null
        || issues != null;
  }
}
