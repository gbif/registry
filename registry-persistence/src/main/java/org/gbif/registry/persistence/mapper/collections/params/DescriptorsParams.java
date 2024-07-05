package org.gbif.registry.persistence.mapper.collections.params;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeStatus;

@Getter
@SuperBuilder
public class DescriptorsParams extends CollectionListParams {

  // descriptors fields
  List<String> usageName;
  List<Integer> usageKey;
  List<Rank> usageRank;
  List<Integer> taxonKey;
  @Nullable List<Country> descriptorCountry;
  @Nullable RangeParam individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable Date dateIdentified;
  @Nullable Date dateIdentifiedFrom;
  @Nullable Date dateIdentifiedBefore;
  @Nullable List<TypeStatus> typeStatus;
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
        || dateIdentified != null
        || dateIdentifiedFrom != null
        || dateIdentifiedBefore != null
        || typeStatus != null
        || recordedBy != null
        || discipline != null
        || objectClassification != null
        || issues != null;
  }
}
