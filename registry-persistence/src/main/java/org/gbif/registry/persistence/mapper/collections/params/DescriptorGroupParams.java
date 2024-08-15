package org.gbif.registry.persistence.mapper.collections.params;

import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;
import org.gbif.api.model.common.paging.Pageable;

@Getter
@Builder
public class DescriptorGroupParams {

  UUID collectionKey;
  @Nullable String query;
  @Nullable String title;
  @Nullable String description;
  @Nullable Boolean deleted;
  @Nullable Pageable page;
}
