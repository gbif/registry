package org.gbif.registry.persistence.mapper.params;

import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class NetworkListParams extends BaseListParams {

  @Nullable private UUID datasetKey;

  public static NetworkListParams from(BaseListParams params) {
    return BaseListParams.copy(NetworkListParams.builder().build(), params);
  }
}
