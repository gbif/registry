/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
