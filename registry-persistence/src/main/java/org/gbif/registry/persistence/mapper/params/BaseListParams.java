package org.gbif.registry.persistence.mapper.params;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.vocabulary.IdentifierType;

import java.util.Date;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class BaseListParams {
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

  public static <T extends BaseListParams> T copy(T copy, BaseListParams other) {
    copy.setDeleted(other.getDeleted());
    copy.setIdentifierType(other.getIdentifierType());
    copy.setIdentifier(other.getIdentifier());
    copy.setMtNamespace(other.getMtNamespace());
    copy.setMtName(other.getMtName());
    copy.setMtValue(other.getMtValue());
    copy.setQuery(other.getQuery());
    copy.setFrom(other.getFrom());
    copy.setTo(other.getTo());
    copy.setPage(other.getPage());
    return copy;
  }
}
