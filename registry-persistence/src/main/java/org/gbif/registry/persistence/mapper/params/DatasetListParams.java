package org.gbif.registry.persistence.mapper.params;

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;

import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class DatasetListParams extends BaseListParams {

  @Nullable private DatasetType type;
  @Nullable private UUID installationKey;
  @Nullable private Country country;
}
