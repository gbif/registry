package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.ih.IHEntity;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class DiffResult {

  @Singular(value = "institutionNoChange")
  private List<Institution> institutionsNoChange;

  @Singular(value = "institutionToCreate")
  private List<Institution> institutionsToCreate;

  @Singular(value = "institutionToUpdate")
  private List<EntityDiffResult<Institution>> institutionsToUpdate;

  @Singular(value = "collectionNoChange")
  private List<Collection> collectionsNoChange;

  @Singular(value = "collectionToUpdate")
  private List<EntityDiffResult<Collection>> collectionsToUpdate;

  @Singular(value = "outdatedInstitution")
  private List<IHOutdated<IHInstitution, CollectionEntity>> outdatedIHInstitutions;

  @Singular(value = "conflict")
  private List<Conflict<IHInstitution, CollectionEntity>> conflicts;

  @Singular(value = "action")
  private List<FailedAction> failedActions;

  @Data
  @AllArgsConstructor
  @Builder
  public static class EntityDiffResult<T extends CollectionEntity> {
    private T oldEntity;
    private T newEntity;
    private StaffDiffResult<T> staffDiffResult;

    public boolean isEmpty() {
      return oldEntity == null && newEntity == null && staffDiffResult.isEmpty();
    }
  }

  @Data
  @Builder
  public static class StaffDiffResult<T extends CollectionEntity> {
    private T entity;

    @Singular(value = "personNoChange")
    private List<Person> personsNoChange;

    @Singular(value = "personToCreate")
    private List<Person> personsToCreate;

    @Singular(value = "personToUpdate")
    private List<PersonDiffResult> personsToUpdate;

    @Singular(value = "personToRemoveFromEntity")
    private List<Person> personsToRemoveFromEntity;

    @Singular(value = "outdatedStaff")
    private List<IHOutdated<IHStaff, Person>> outdatedStaff;

    @Singular(value = "conflict")
    private List<Conflict<IHStaff, Person>> conflicts;

    public boolean isEmpty() {
      return personsNoChange.isEmpty()
          && personsToCreate.isEmpty()
          && personsToUpdate.isEmpty()
          && personsToRemoveFromEntity.isEmpty()
          && outdatedStaff.isEmpty()
          && conflicts.isEmpty();
    }
  }

  @Data
  @AllArgsConstructor
  public static class PersonDiffResult {
    private Person oldPerson;
    private Person newPerson;
  }

  @Data
  @AllArgsConstructor
  public static class FailedAction {
    private Object entity;
    private String message;
  }

  @Data
  @AllArgsConstructor
  public static class IHOutdated<T extends IHEntity, R extends CollectionEntity> {
    private T ihEntity;
    private R grSciCollEntity;
  }

  @Data
  @AllArgsConstructor
  public static class Conflict<T extends IHEntity, R extends CollectionEntity> {
    private T ihEntity;
    private List<R> grSciCollEntities;
  }
}
