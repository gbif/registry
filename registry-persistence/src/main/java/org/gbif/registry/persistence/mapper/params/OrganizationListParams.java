package org.gbif.registry.persistence.mapper.params;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.Date;
import java.util.UUID;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
// TODO: make super class??
public class OrganizationListParams {

  @Nullable private Boolean isEndorsed;
  @Nullable private UUID networkKey;
  @Nullable private UUID installationKey;
  @Nullable private Country country;
  @Nullable private Boolean deleted;
  @Nullable private IdentifierType identifierType;
  @Nullable private String identifier;
  @Nullable private String mtNamespace; // namespace
  @Nullable private String mtName; // name
  @Nullable private String mtValue; // value
  @Nullable private String query; // query
  @Nullable private Date from;
  @Nullable private Date to;
  @Nullable private Pageable page;
}
