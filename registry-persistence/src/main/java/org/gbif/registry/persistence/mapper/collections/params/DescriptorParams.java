package org.gbif.registry.persistence.mapper.collections.params;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;

@Getter
@Builder
public class DescriptorParams {

  @Nullable String query;
  @Nullable Long descriptorGroupKey;
  @Nullable List<Integer> usageKey;
  @Nullable List<String> usageName;
  @Nullable List<Rank> usageRank;
  @Nullable List<Integer> taxonKey;
  @Nullable List<Country> country;
  @Nullable RangeParam individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable Date dateIdentifiedFrom;
  @Nullable Date dateIdentifiedBefore;
  @Nullable List<String> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> issues;
  @Nullable Pageable page;
}
