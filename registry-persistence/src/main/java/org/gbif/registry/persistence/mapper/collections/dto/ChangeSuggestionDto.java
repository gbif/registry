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

public class ChangeSuggestionDto {

  private Integer key;
  private CollectionEntityType collectionEntityType;
  private UUID entityKey;
  private Type type;
  private Status status;
  private String proposedBy;
  private Date proposed;
  private String appliedBy;
  private Date applied;
  private String discardedBy;
  private Date discarded;
  private Country country;
  private String suggestedEntity;
  private Set<ChangeDto> changes = new HashSet<>();
  private List<String> comments = new ArrayList<>();
  private UUID mergeTargetKey;
  private UUID institutionConvertedCollection;
  private String nameNewInstitutionConvertedCollection;
  private Date modified;
  private String modifiedBy;

  public Integer getKey() {
    return key;
  }

  public void setKey(Integer key) {
    this.key = key;
  }

  public CollectionEntityType getCollectionEntityType() {
    return collectionEntityType;
  }

  public void setCollectionEntityType(CollectionEntityType collectionEntityType) {
    this.collectionEntityType = collectionEntityType;
  }

  public UUID getEntityKey() {
    return entityKey;
  }

  public void setEntityKey(UUID entityKey) {
    this.entityKey = entityKey;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getProposedBy() {
    return proposedBy;
  }

  public void setProposedBy(String proposedBy) {
    this.proposedBy = proposedBy;
  }

  public Date getProposed() {
    return proposed;
  }

  public void setProposed(Date proposed) {
    this.proposed = proposed;
  }

  public String getAppliedBy() {
    return appliedBy;
  }

  public void setAppliedBy(String appliedBy) {
    this.appliedBy = appliedBy;
  }

  public Date getApplied() {
    return applied;
  }

  public void setApplied(Date applied) {
    this.applied = applied;
  }

  public String getDiscardedBy() {
    return discardedBy;
  }

  public void setDiscardedBy(String discardedBy) {
    this.discardedBy = discardedBy;
  }

  public Date getDiscarded() {
    return discarded;
  }

  public void setDiscarded(Date discarded) {
    this.discarded = discarded;
  }

  public Country getCountry() {
    return country;
  }

  public void setCountry(Country country) {
    this.country = country;
  }

  public String getSuggestedEntity() {
    return suggestedEntity;
  }

  public void setSuggestedEntity(String suggestedEntity) {
    this.suggestedEntity = suggestedEntity;
  }

  public Set<ChangeDto> getChanges() {
    return changes;
  }

  public void setChanges(Set<ChangeDto> changes) {
    this.changes = changes;
  }

  public List<String> getComments() {
    return comments;
  }

  public void setComments(List<String> comments) {
    this.comments = comments;
  }

  public UUID getMergeTargetKey() {
    return mergeTargetKey;
  }

  public void setMergeTargetKey(UUID mergeTargetKey) {
    this.mergeTargetKey = mergeTargetKey;
  }

  public UUID getInstitutionConvertedCollection() {
    return institutionConvertedCollection;
  }

  public void setInstitutionConvertedCollection(UUID institutionConvertedCollection) {
    this.institutionConvertedCollection = institutionConvertedCollection;
  }

  public String getNameNewInstitutionConvertedCollection() {
    return nameNewInstitutionConvertedCollection;
  }

  public void setNameNewInstitutionConvertedCollection(
      String nameNewInstitutionConvertedCollection) {
    this.nameNewInstitutionConvertedCollection = nameNewInstitutionConvertedCollection;
  }

  public Date getModified() {
    return modified;
  }

  public void setModified(Date modified) {
    this.modified = modified;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public void setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
  }
}
