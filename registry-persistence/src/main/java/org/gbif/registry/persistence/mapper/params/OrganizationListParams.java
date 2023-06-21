package org.gbif.registry.persistence.mapper.params;

import org.gbif.api.vocabulary.Country;

import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class OrganizationListParams extends BaseListParams {

  @Nullable private Boolean isEndorsed;
  @Nullable private UUID networkKey;
  @Nullable private UUID installationKey;
  @Nullable private Country country;
  @Nullable private UUID endorsedByNodeKey;

  public static OrganizationListParams from(BaseListParams params) {
    return BaseListParams.copy(OrganizationListParams.builder().build(), params);
  }
}
