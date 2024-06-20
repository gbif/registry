package org.gbif.registry.persistence.mapper.collections.params;

import lombok.Builder;
import lombok.Getter;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.TypeStatus;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

@Getter
@Builder
public class DescriptorRecordsParams {

  @Nullable String query;
  @Nullable Long descriptorKey;
  @Nullable List<String> scientificName;
  @Nullable List<Country> country;
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
  @Nullable Pageable page;
}
