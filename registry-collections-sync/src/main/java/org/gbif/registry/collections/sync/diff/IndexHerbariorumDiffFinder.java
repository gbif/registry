package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;
import org.gbif.registry.collections.sync.notification.Issue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.UpdateDiffResult;
import static org.gbif.registry.collections.sync.diff.Utils.encodeIRN;
import static org.gbif.registry.collections.sync.diff.Utils.isIHOutdated;

/**
 * A synchronization utility that will ensure GRSciColl is up to date with IndexHerbariorum. This
 * operates as follows:
 *
 * <ul>
 *   <li>Retrieve all Herbaria from IndexHerbariorum
 *   <li>For each entity locate the equivalent Institution or Collection in GRSciColl using the IH
 *       IRN
 *   <li>If the entity exists and they differ, update GrSciColl
 *   <li>If the entity does not exist, insert it as an institution and with an identifier holding
 *       the IH IRN
 * </ul>
 *
 * <p>A future version of this may allow editing of IH entities in GRSciColl. Under that scenario
 * when entities differ more complex logic is required, likely requiring notification to GRSciColl
 * and IH staff to resolve the differences.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexHerbariorumDiffFinder {

  public static DiffResult syncIH(
      List<IHInstitution> ihInstitutions,
      Function<String, List<IHStaff>> ihStaffFetcher,
      List<Institution> institutions,
      List<Collection> collections) {

    DiffResult.DiffResultBuilder diffResult = DiffResult.builder();
    List<Institution> institutionsCopy = new ArrayList<>(institutions);
    List<Collection> collectionsCopy = new ArrayList<>(collections);
    for (IHInstitution ihInstitution : ihInstitutions) {

      // locate potential matches in GrSciColl
      Match match =
          findMatches(institutionsCopy, collectionsCopy, encodeIRN(ihInstitution.getIrn()));

      if (match.onlyOneInstitutionMatch()) {
        Institution existing = match.institutions.iterator().next();
        log.info("Institution {} matched with IH {}", existing.getKey(), ihInstitution.getCode());
        institutionsCopy.remove(existing);

        if (isIHOutdated(ihInstitution.getDateModified(), existing)) {
          // add issue
          diffResult.institutionConflict(
              Issue.createOutdatedIHInstitutionIssue(existing, ihInstitution));
          continue;
        }

        // we look for differences between entities
        Institution institution =
            EntityConverter.createInstitution()
                .fromIHInstitution(ihInstitution)
                .withExisting(existing)
                .convert();

        UpdateDiffResult.UpdateDiffResultBuilder<Institution> updateDiffBuilder =
            UpdateDiffResult.builder();
        if (!institution.lenientEquals(existing)) {
          updateDiffBuilder.newEntity(institution).oldEntity(existing);
        }

        // look for differences in staff
        log.info("Calling IH WS to get staff from institution {}", ihInstitution.getCode());
        updateDiffBuilder.staffDiffResult(
            StaffDiffFinder.syncStaff(
                ihStaffFetcher.apply(ihInstitution.getCode()), institution.getContacts()));

        UpdateDiffResult<Institution> updateDiff = updateDiffBuilder.build();
        if (updateDiff.isEmpty()) {
          diffResult.institutionNoChange(existing);
        } else {
          diffResult.institutionToUpdate(updateDiff);
        }

      } else if (match.onlyOneCollectionMatch()) {
        Collection existing = match.collections.iterator().next();
        log.info("Collection {} matched with IH {}", existing.getKey(), ihInstitution.getCode());
        collectionsCopy.remove(existing);

        if (isIHOutdated(ihInstitution.getDateModified(), existing)) {
          diffResult.collectionConflict(
              Issue.createOutdatedIHCollectionIssue(existing, ihInstitution));
          continue;
        }

        // we look for differences between entities
        Collection collection =
            EntityConverter.createCollection()
                .fromIHInstitution(ihInstitution)
                .withExisting(existing)
                .convert();

        UpdateDiffResult.UpdateDiffResultBuilder<Collection> updateDiffBuilder =
            UpdateDiffResult.builder();
        if (!collection.lenientEquals(existing)) {
          updateDiffBuilder.newEntity(collection).oldEntity(existing);
        }

        // look for differences in staff
        updateDiffBuilder.staffDiffResult(
            StaffDiffFinder.syncStaff(
                ihStaffFetcher.apply(ihInstitution.getCode()), collection.getContacts()));

        UpdateDiffResult<Collection> updateDiff = updateDiffBuilder.build();
        if (updateDiff.isEmpty()) {
          diffResult.collectionNoChange(existing);
        } else {
          diffResult.collectionToUpdate(updateDiff);
        }

      } else if (match.noMatches()) {
        log.info("New institution to create for IH: {}", ihInstitution.getCode());
        // create institution
        Institution institution =
            EntityConverter.createInstitution().fromIHInstitution(ihInstitution).convert();

        institution.setContacts(
            ihStaffFetcher.apply(ihInstitution.getCode()).stream()
                .map(s -> EntityConverter.createPerson().fromIHStaff(s).convert())
                .collect(Collectors.toList()));

        diffResult.institutionToCreate(institution);

      } else {
        // Conflict that needs resolved manually
        log.info(
            "Conflict. {} institutions and {} collections are candidate matches in registry for {}: ",
            match.institutions,
            match.collections,
            ihInstitution.getOrganization());

        diffResult.conflict(Issue.createConflict(match.getAllMatches(), ihInstitution));
        institutionsCopy.removeAll(match.institutions);
        collectionsCopy.removeAll(match.collections);
      }
    }

    return diffResult.build();
  }

  /** Filters the source to only those having the given ID. */
  private static <T extends Identifiable> Set<T> filterById(List<T> source, String id) {
    return source.stream()
        .filter(
            o -> o.getIdentifiers().stream().anyMatch(i -> Objects.equals(id, i.getIdentifier())))
        .collect(Collectors.toSet());
  }

  private static Match findMatches(
      List<Institution> institutions, List<Collection> collections, String irn) {
    CompletableFuture<Set<Institution>> matchedInstitutionsFuture =
        CompletableFuture.supplyAsync(() -> filterById(institutions, irn));
    CompletableFuture<Set<Collection>> matchedCollectionsFuture =
        CompletableFuture.supplyAsync(() -> filterById(collections, irn));
    CompletableFuture.allOf(matchedInstitutionsFuture, matchedCollectionsFuture).join();

    Match match = new Match();
    match.institutions = matchedInstitutionsFuture.join();
    match.collections = matchedCollectionsFuture.join();

    return match;
  }

  private static class Match {
    Set<Institution> institutions;
    Set<Collection> collections;

    boolean onlyOneInstitutionMatch() {
      return institutions.size() == 1 && collections.isEmpty();
    }

    boolean onlyOneCollectionMatch() {
      return collections.size() == 1 && institutions.isEmpty();
    }

    boolean noMatches() {
      return institutions.isEmpty() && collections.isEmpty();
    }

    List<CollectionEntity> getAllMatches() {
      List<CollectionEntity> all = new ArrayList<>(institutions);
      all.addAll(collections);
      return all;
    }
  }
}
