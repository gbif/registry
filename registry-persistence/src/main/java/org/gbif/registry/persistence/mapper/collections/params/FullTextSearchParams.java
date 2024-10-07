package org.gbif.registry.persistence.mapper.collections.params;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import org.gbif.api.vocabulary.Country;

@Data
@Builder
public class FullTextSearchParams {

  @Nullable String query;
  boolean highlight;
  @Nullable String type;
  @Nullable Boolean displayOnNHCPortal;
  @Nullable Country country;
  int limit;
}
