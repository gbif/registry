package org.gbif.registry.persistence.mapper.params;

import lombok.experimental.SuperBuilder;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;

@SuperBuilder
@Getter
public class OrganizationListParams extends BaseListParams {

  @Nullable private Boolean isEndorsed;
  @Nullable private UUID networkKey;
  @Nullable private UUID installationKey;
  @Nullable private Country country;
}
