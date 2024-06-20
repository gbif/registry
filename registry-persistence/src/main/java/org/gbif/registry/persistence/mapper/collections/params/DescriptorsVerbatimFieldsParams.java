package org.gbif.registry.persistence.mapper.collections.params;

import org.gbif.api.model.common.paging.Pageable;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DescriptorsVerbatimFieldsParams {

  @Nullable String query;
  @Nullable Long recordKey;
  @Nullable Pageable page;
}
