package org.gbif.registry.persistence.mapper.collections.params;

import lombok.Builder;
import lombok.Getter;
import org.gbif.api.model.common.paging.Pageable;

import javax.annotation.Nullable;
import java.util.UUID;

@Getter
@Builder
public class DescriptorsParams {

  // TODO: add deleted param

  UUID collectionKey;
  @Nullable String query;
  @Nullable String title;
  @Nullable String description;
  @Nullable Pageable page;
}
