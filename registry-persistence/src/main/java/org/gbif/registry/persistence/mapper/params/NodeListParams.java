package org.gbif.registry.persistence.mapper.params;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class NodeListParams extends BaseListParams {

  public static NodeListParams from(BaseListParams params) {
    return BaseListParams.copy(NodeListParams.builder().build(), params);
  }

}
