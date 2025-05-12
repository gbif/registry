package org.gbif.registry.persistence.mapper.collections.params;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;

@Getter
@Builder
public class DescriptorParams {

  @Nullable String query;
  @Nullable Long descriptorGroupKey;
  @Nullable List<String> usageKey;
  @Nullable List<String> usageName;
  @Nullable List<String> usageRank;
  @Nullable List<String> taxonKey;
  @Nullable List<Country> country;
  @Nullable List<RangeParam<Integer>> individualCount;
  @Nullable List<String> identifiedBy;
  @Nullable List<RangeParam<LocalDate>> dateIdentified;
  @Nullable List<String> typeStatus;
  @Nullable List<String> recordedBy;
  @Nullable List<String> discipline;
  @Nullable List<String> objectClassification;
  @Nullable List<String> issues;
  @Nullable Pageable page;
}
