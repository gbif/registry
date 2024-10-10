package org.gbif.registry.persistence.mapper.collections.params;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;

@Getter
@SuperBuilder
public class DescriptorsParams extends CollectionListParams {

  // descriptors fields
  List<String> usageName;
  List<String> usageKey;
  List<String> usageRank;
  List<String> taxonKey;
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
