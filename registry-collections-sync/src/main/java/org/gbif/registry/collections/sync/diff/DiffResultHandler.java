package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.registry.collections.sync.SyncConfig;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.notification.GithubClient;
import org.gbif.registry.collections.sync.notification.Issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.FailedAction;
import static org.gbif.registry.collections.sync.diff.DiffResult.PersonDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.EntityDiffResult;

/**
 * Handles the results stored in a {@link org.gbif.registry.collections.sync.diff.DiffResult}. This
 * class is responsible to make the necessary updates in GrSciColl and notify the existing
 * conflicts.
 */
@Builder
@Slf4j
public class DiffResultHandler {
  private SyncConfig config;
  private DiffResult diffResult;
  private GrSciCollHttpClient grSciCollHttpClient;
  private GithubClient githubClient;

  public List<FailedAction> handle() {
    if (config.isDryRun() && config.isIgnoreConflicts()) {
      log.info("Skipping results handler. Dry run and ignore conflicts are both set to true.");
      return Collections.emptyList();
    }

    List<FailedAction> fails = new ArrayList<>();
    fails.addAll(
        executeGrSciCollAction(
            diffResult.getInstitutionsToCreate(),
            e -> grSciCollHttpClient.createInstitution(e),
            ActionType.CREATE));
    fails.addAll(
        executeGrSciCollUpdateAction(
            diffResult.getInstitutionsToUpdate(), e -> grSciCollHttpClient.updateInstitution(e)));
    fails.addAll(
        executeGrSciCollUpdateAction(
            diffResult.getCollectionsToUpdate(), e -> grSciCollHttpClient.updateCollection(e)));

    if (!config.isIgnoreConflicts()) {
      fails.addAll(executeConflictAction(diffResult.getInstitutionConflicts()));
      fails.addAll(executeConflictAction(diffResult.getCollectionConflicts()));
      fails.addAll(executeConflictAction(diffResult.getConflicts()));
    }

    return fails;
  }

  private List<FailedAction> handleStaffDiff(StaffDiffResult s) {
    List<FailedAction> fails = new ArrayList<>();
    fails.addAll(
        executeGrSciCollAction(
            s.getPersonsToCreate(), e -> grSciCollHttpClient.createPerson(e), ActionType.CREATE));
    fails.addAll(
        executeGrSciCollAction(
            s.getPersonsToDelete(), e -> grSciCollHttpClient.deletePerson(e), ActionType.DELETE));
    fails.addAll(
        executeGrSciCollAction(
            s.getPersonsToUpdate().stream()
                .map(PersonDiffResult::getNewPerson)
                .collect(Collectors.toList()),
            e -> grSciCollHttpClient.updatePerson(e),
            ActionType.UPDATE));
    fails.addAll(executeConflictAction(s.getConflicts()));

    return fails;
  }

  private List<FailedAction> executeConflictAction(List<Issue> issues) {
    List<FailedAction> fails = new ArrayList<>();
    if (config.isIgnoreConflicts()) {
      log.debug("Ignore conflicts flag enabled. Ignoring conflict.");
      return fails;
    }
    issues.forEach(
        i ->
            fails.addAll(
                executeAndStoreFail(e -> githubClient.createIssue(e), i, ActionType.CREATE)));
    return fails;
  }

  private <T> List<FailedAction> executeGrSciCollAction(
      List<T> entities, Consumer<T> action, ActionType actionType) {
    List<FailedAction> fails = new ArrayList<>();
    if (config.isDryRun()) {
      log.debug("Dry run enabled. Ignoring update.");
      return fails;
    }
    entities.forEach(e -> fails.addAll(executeAndStoreFail(action, e, actionType)));
    return fails;
  }

  private <T extends CollectionEntity> List<FailedAction> executeGrSciCollUpdateAction(
    List<EntityDiffResult<T>> diffs, Consumer<T> updateAction) {
    List<FailedAction> fails = new ArrayList<>();
    if (config.isDryRun()) {
      log.debug("Dry run enabled. Ignoring update.");
      return fails;
    }

    diffs.forEach(
        d -> {
          fails.addAll(executeAndStoreFail(updateAction, d.getNewEntity(), ActionType.UPDATE));
          fails.addAll(handleStaffDiff(d.getStaffDiffResult()));

          if (!config.isIgnoreConflicts()) {
            d.getStaffDiffResult()
                .getConflicts()
                .forEach(
                    c ->
                        fails.addAll(
                            executeAndStoreFail(
                                i -> githubClient.createIssue(i), c, ActionType.CREATE)));
          }
        });

    return fails;
  }

  private <T> List<FailedAction> executeAndStoreFail(
      Consumer<T> action, T entity, ActionType actionType) {
    List<FailedAction> fails = new ArrayList<>();
    boolean success = executeSilently(() -> action.accept(entity));
    if (!success) {
      fails.add(
          new FailedAction(
              entity, entity.getClass().getSimpleName() + " " + actionType.name() + " failed"));
    }

    return fails;
  }

  private boolean executeSilently(Runnable runnable) {
    try {
      runnable.run();
      return true;
    } catch (Exception ex) {
      log.error("Error updating GrSciColl from IH sync", ex);
      return false;
    }
  }

  private enum ActionType {
    UPDATE,
    CREATE,
    DELETE;
  }
}
