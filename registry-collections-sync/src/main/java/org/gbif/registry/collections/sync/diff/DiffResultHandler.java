package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.SyncConfig;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.notification.GithubClient;
import org.gbif.registry.collections.sync.notification.Issue;
import org.gbif.registry.collections.sync.notification.IssueFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.EntityDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.FailedAction;
import static org.gbif.registry.collections.sync.diff.DiffResult.PersonDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;

/**
 * Handles the results stored in a {@link DiffResult}. This class is responsible to make the
 * necessary updates in GrSciColl and notify the existing conflicts.
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

    // Institutions to create
    for (Institution institutionToCreate : diffResult.getInstitutionsToCreate()) {
      executeOrCreateFail(
              () -> grSciCollHttpClient.createInstitution(institutionToCreate),
              e ->
                  new FailedAction(
                      institutionToCreate, "Failed to create institution: " + e.getMessage()))
          .ifPresent(fails::add);
    }

    // Institutions to update
    for (EntityDiffResult<Institution> diff : diffResult.getInstitutionsToUpdate()) {
      executeOrCreateFail(
              () -> grSciCollHttpClient.updateInstitution(diff.getNewEntity()),
              e ->
                  new FailedAction(
                      diff.getNewEntity(), "Failed to update institution: " + e.getMessage()))
          .ifPresent(fails::add);

      // staff
      fails.addAll(
          handleStaffDiff(
              diff.getStaffDiffResult(),
              grSciCollHttpClient::addPersonToInstitution,
              grSciCollHttpClient::removePersonFromInstitution));
    }

    // collections to update
    for (EntityDiffResult<Collection> diff : diffResult.getCollectionsToUpdate()) {
      executeOrCreateFail(
              () -> grSciCollHttpClient.updateCollection(diff.getNewEntity()),
              e ->
                  new FailedAction(
                      diff.getNewEntity(), "Failed to update collection: " + e.getMessage()))
          .ifPresent(fails::add);

      // staff
      fails.addAll(
          handleStaffDiff(
              diff.getStaffDiffResult(),
              grSciCollHttpClient::addPersonToCollection,
              grSciCollHttpClient::removePersonFromCollection));
    }

    // issues and conflicts
    fails.addAll(createInstitutionIssues());

    // fails
    if (!fails.isEmpty()) {
      // create issue
      createIssue(issueFactory.createFailsNotification(fails)).ifPresent(fails::add);
    }

    return fails;
  }

  private <T extends CollectionEntity> List<FailedAction> handleStaffDiff(
      StaffDiffResult<T> staffDiffResult,
      BiConsumer<UUID, UUID> addPersonAction,
      BiConsumer<UUID, UUID> removePersonAction) {
    List<FailedAction> fails = new ArrayList<>();

    for (Person personToCreate : staffDiffResult.getPersonsToCreate()) {
      executeOrCreateFail(
              () -> {
                UUID createdKey = grSciCollHttpClient.createPerson(personToCreate);
                addPersonAction.accept(createdKey, staffDiffResult.getEntity().getKey());
              },
              e -> new FailedAction(personToCreate, "Failed to add person: " + e.getMessage()))
          .ifPresent(fails::add);
    }

    for (PersonDiffResult personDiff : staffDiffResult.getPersonsToUpdate()) {
      executeOrCreateFail(
              () -> {
                grSciCollHttpClient.updatePerson(personDiff.getNewPerson());
                // add identifiers if needed
                personDiff.getNewPerson().getIdentifiers().stream()
                    .filter(i -> i.getKey() == null)
                    .forEach(
                        i ->
                            grSciCollHttpClient.addIdentifierToPerson(
                                personDiff.getNewPerson().getKey(), i));
              },
              e ->
                  new FailedAction(
                      personDiff.getNewPerson(), "Failed to update person: " + e.getMessage()))
          .ifPresent(fails::add);
    }

    for (Person personToRemove : staffDiffResult.getPersonsToRemoveFromEntity()) {
      executeOrCreateFail(
              () ->
                  removePersonAction.accept(
                      personToRemove.getKey(), staffDiffResult.getEntity().getKey()),
              e -> new FailedAction(personToRemove, "Failed to remove person: " + e.getMessage()))
          .ifPresent(fails::add);
    }

    // staff issues
    fails.addAll(createStaffIssues(staffDiffResult));

    return fails;
  }

  private Optional<FailedAction> executeOrCreateFail(
      Runnable runnable, Function<Exception, FailedAction> failCreator) {
    if (config.isDryRun()) {
      return Optional.empty();
    }

    try {
      runnable.run();
    } catch (Exception e) {
      return Optional.of(failCreator.apply(e));
    }

    return Optional.empty();
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

    issues.forEach(i -> createIssue(i).ifPresent(fails::add));

    return fails;
  }

  private <T extends CollectionEntity> List<FailedAction> createStaffIssues(
      StaffDiffResult<T> staffDiffResult) {
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

    issues.forEach(i -> createIssue(i).ifPresent(fails::add));
    return fails;
  }

  private Optional<FailedAction> createIssue(Issue issue) {
    if (!config.isSendNotifications()) {
      return Optional.empty();
    }

    try {
      githubClient.createIssue(issue);
    } catch (Exception e) {
      return Optional.of(new FailedAction(issue, "Failed to create isssue: " + e.getMessage()));
    }

    return Optional.empty();
  }
}
