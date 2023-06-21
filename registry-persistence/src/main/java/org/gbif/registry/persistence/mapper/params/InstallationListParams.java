package org.gbif.registry.persistence.mapper.params;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import org.gbif.api.vocabulary.InstallationType;

import javax.annotation.Nullable;

import java.util.UUID;

@SuperBuilder
@Getter
public class InstallationListParams extends BaseListParams {

  @Nullable private InstallationType type;
  @Nullable private UUID organizationKey;
  @Nullable private UUID endorsedByNodeKey;

  public static InstallationListParams from(BaseListParams params) {
    return BaseListParams.copy(InstallationListParams.builder().build(), params);
  }
}
