package org.gbif.registry.persistence.mapper.collections.params;

import java.util.List;
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
  @Nullable List<Boolean> displayOnNHCPortal;
  @Nullable List<Country> country;
  int limit;
}
