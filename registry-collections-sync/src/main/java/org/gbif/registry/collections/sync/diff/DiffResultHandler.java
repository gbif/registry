package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.registry.collections.sync.SyncConfig;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.notification.GithubClient;
import org.gbif.registry.collections.sync.notification.Issue;
import org.gbif.registry.collections.sync.notification.IssueFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.EntityDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.FailedAction;
import static org.gbif.registry.collections.sync.diff.DiffResult.PersonDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;

/**
 * Handles the results stored in a {@link org.gbif.registry.collections.sync.diff.DiffResult}. This
 * class is responsible to make the necessary updates in GrSciColl and notify the existing
 * conflicts.
 */
@Slf4j
public class DiffResultHandler {
  private final DiffResult diffResult;
  private final SyncConfig config;
  private final GrSciCollHttpClient grSciCollHttpClient;
  private final IssueFactory issueFactory;
  @Nullable private GithubClient githubClient;

  @Builder
  private DiffResultHandler(
      DiffResult diffResult, SyncConfig config, GrSciCollHttpClient grSciCollHttpClient) {
    this.diffResult = Objects.requireNonNull(diffResult);
    this.config = Objects.requireNonNull(config);
    this.grSciCollHttpClient = Objects.requireNonNull(grSciCollHttpClient);

    if (config.isSendNotifications()) {
      this.githubClient = GithubClient.create(config);
    }

    this.issueFactory = IssueFactory.fromConfig(config.getNotification());
  }

  public List<FailedAction> handle() {
    if (config.isDryRun() && !config.isSendNotifications()) {
      log.info("Skipping results handler. Dry run and ignore conflicts are both set to true.");
      return Collections.emptyList();
    }

    List<FailedAction> fails = new ArrayList<>();
    fails.addAll(
        executeGrSciCollAction(
            diffResult.getInstitutionsToCreate(),
            grSciCollHttpClient::createInstitution,
            ActionType.CREATE));
    fails.addAll(
        executeGrSciCollUpdateAction(
            diffResult.getInstitutionsToUpdate(), grSciCollHttpClient::updateInstitution));
    fails.addAll(
        executeGrSciCollUpdateAction(
            diffResult.getCollectionsToUpdate(), grSciCollHttpClient::updateCollection));
    fails.addAll(createInstitutionIssues());

    if (!fails.isEmpty() && config.isSendNotifications()) {
      // create issue
      githubClient.createIssue(issueFactory.createFailsNotification(fails));
    }

    return fails;
  }

  private List<FailedAction> createInstitutionIssues() {
    List<FailedAction> fails = new ArrayList<>();
    if (!config.isSendNotifications()) {
      log.debug("Ignore conflicts flag enabled. Ignoring conflict.");
      return fails;
    }

    List<Issue> issues =
        diffResult.getOutdatedIHInstitutions().stream()
            .map(
                o ->
                    issueFactory.createOutdatedIHInstitutionIssue(
                        o.getGrSciCollEntity(), o.getIhEntity()))
            .collect(Collectors.toList());

    issues.addAll(
        diffResult.getConflicts().stream()
            .map(c -> issueFactory.createConflict(c.getGrSciCollEntities(), c.getIhEntity()))
            .collect(Collectors.toList()));

    issues.forEach(
        i -> fails.addAll(executeAndStoreFail(githubClient::createIssue, i, ActionType.CREATE)));

    return fails;
  }

  private List<FailedAction> handleStaffDiff(StaffDiffResult s) {
    List<FailedAction> fails = new ArrayList<>();
    fails.addAll(
        executeGrSciCollAction(
            s.getPersonsToCreate(), grSciCollHttpClient::createPerson, ActionType.CREATE));
    fails.addAll(
        executeGrSciCollAction(
            s.getPersonsToDelete(), grSciCollHttpClient::deletePerson, ActionType.DELETE));
    fails.addAll(
        executeGrSciCollAction(
            s.getPersonsToUpdate().stream()
                .map(PersonDiffResult::getNewPerson)
                .collect(Collectors.toList()),
            grSciCollHttpClient::updatePerson,
            ActionType.UPDATE));
    fails.addAll(createStaffIssues(s));

    return fails;
  }

  private List<FailedAction> createStaffIssues(StaffDiffResult staffDiffResult) {
    List<FailedAction> fails = new ArrayList<>();
    if (!config.isSendNotifications()) {
      log.debug("Ignore conflicts flag enabled. Ignoring conflict.");
      return fails;
    }

    List<Issue> issues =
        staffDiffResult.getOutdatedStaff().stream()
            .map(
                o ->
                    issueFactory.createOutdatedIHStaffIssue(
                        o.getGrSciCollEntity(), o.getIhEntity()))
            .collect(Collectors.toList());

    issues.addAll(
        staffDiffResult.getConflicts().stream()
            .map(c -> issueFactory.createStaffConflict(c.getGrSciCollEntities(), c.getIhEntity()))
            .collect(Collectors.toList()));

    issues.forEach(
        i -> fails.addAll(executeAndStoreFail(githubClient::createIssue, i, ActionType.CREATE)));
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
        });

    return fails;
  }

  private <T> List<FailedAction> executeAndStoreFail(
      Consumer<T> action, T entity, ActionType actionType) {
    List<FailedAction> fails = new ArrayList<>();

    try {
      action.accept(entity);
    } catch (Exception ex) {
      fails.add(
          new FailedAction(
              entity,
              entity.getClass().getSimpleName()
                  + " "
                  + actionType.name()
                  + " failed: "
                  + ex.getMessage()));
    }

    return fails;
  }

  private enum ActionType {
    UPDATE,
    CREATE,
    DELETE;
  }
}
