package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.notification.Issue;

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
  private List<UpdateDiffResult<Institution>> institutionsToUpdate;

  @Singular(value = "institutionConflict")
  private List<Issue> institutionConflicts;

  @Singular(value = "collectionNoChange")
  private List<Collection> collectionsNoChange;

  @Singular(value = "collectionToUpdate")
  private List<UpdateDiffResult<Collection>> collectionsToUpdate;

  @Singular(value = "collectionConflict")
  private List<Issue> collectionConflicts;

  @Singular(value = "conflict")
  private List<Issue> conflicts;

  @Singular(value = "action")
  private List<FailedAction> failedActions;

  public boolean isEmpty() {
    return institutionsNoChange.isEmpty()
        && institutionsToCreate.isEmpty()
        && institutionsToUpdate.isEmpty()
        && collectionsNoChange.isEmpty()
        && collectionsToUpdate.isEmpty()
        && conflicts.isEmpty();
  }

  @Data
  @AllArgsConstructor
  @Builder
  public static class UpdateDiffResult<T extends CollectionEntity> {
    private T oldEntity;
    private T newEntity;
    private StaffDiffResult staffDiffResult;

    public boolean isEmpty() {
      return oldEntity == null && newEntity == null && staffDiffResult.isEmpty();
    }
  }

  @Data
  @Builder
  public static class StaffDiffResult {
    @Singular(value = "personNoChange")
    private List<Person> personsNoChange;

    @Singular(value = "personToCreate")
    private List<Person> personsToCreate;

    @Singular(value = "personToUpdate")
    private List<PersonDiffResult> personsToUpdate;

    @Singular(value = "personToDelete")
    private List<Person> personsToDelete;

    @Singular(value = "conflict")
    private List<Issue> conflicts;

    public boolean isEmpty() {
      return personsNoChange.isEmpty()
          && personsToCreate.isEmpty()
          && personsToUpdate.isEmpty()
          && personsToDelete.isEmpty()
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
}
