package org.gbif.registry.persistence.mapper.params;

import lombok.experimental.SuperBuilder;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.IdentifierType;

import javax.annotation.Nullable;

import java.util.Date;

@SuperBuilder
public class BaseListParams {
  @Nullable
  private Boolean deleted;
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
