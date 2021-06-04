package org.gbif.registry.persistence.mapper.collections.dto;

import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.vocabulary.Country;

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
