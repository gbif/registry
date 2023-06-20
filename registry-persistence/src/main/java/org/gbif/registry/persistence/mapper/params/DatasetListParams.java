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
  @Nullable private String doi;
  @Nullable private UUID publishedByOrgKey;
  @Nullable private UUID parentKey;
  @Nullable private UUID networkKey;
  @Nullable private Boolean isDuplicate;
  @Nullable private Boolean isSubdataset;

  public static DatasetListParams from(BaseListParams params) {
    return BaseListParams.copy(DatasetListParams.builder().build(), params);
  }
}
