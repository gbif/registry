/*
 * Copyright 2020-2021 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.Data;

@Data
public class ChangeSuggestionDto {

  private Integer key;
  private CollectionEntityType entityType;
  private UUID entityKey;
  private Type type;
  private Status status;
  private String proposedBy;
  private String proposerEmail;
  private Date proposed;
  private String appliedBy;
  private Date applied;
  private String discardedBy;
  private Date discarded;
  private String suggestedEntity;
  private Set<ChangeDto> changes = new HashSet<>();
  private List<String> comments = new ArrayList<>();
  private UUID mergeTargetKey;
  private UUID institutionConvertedCollection;
  private String nameNewInstitutionConvertedCollection;
  private Date modified;
  private String modifiedBy;
}
