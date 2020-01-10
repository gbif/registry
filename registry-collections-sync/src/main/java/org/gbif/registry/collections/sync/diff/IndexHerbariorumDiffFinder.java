package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;
import org.gbif.registry.collections.sync.notification.Issue;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.Builder;
import static org.gbif.registry.collections.sync.diff.DiffResult.CollectionDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.InstitutionDiffResult;
import static org.gbif.registry.collections.sync.ih.IHUtils.encodeIRN;

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
public class IndexHerbariorumDiffFinder {

  public static DiffResult syncIH(
      List<IHInstitution> ihInstitutions,
      Function<String, List<IHStaff>> ihStaffSupplier,
      List<Institution> institutions,
      List<Collection> collections) {

    Builder syncResult = new Builder();
    List<Institution> institutionsCopy = new ArrayList<>(institutions);
    List<Collection> collectionsCopy = new ArrayList<>(collections);
    for (IHInstitution ihInstitution : ihInstitutions) {

      // locate potential matches in GrSciColl
      Match match = findMatches(institutions, collections, encodeIRN(ihInstitution.getIrn()));

      if (match.onlyOneInstitutionMatch()) {
        Institution existing = match.institutions.iterator().next();
        institutionsCopy.remove(existing);

        if (isIHOutdated(ihInstitution, existing.getModified())) {
          // add issue
          syncResult.institutionConflict(
              Issue.createOutdatedIHInstitutionIssue(existing, ihInstitution));
          continue;
        }

        // we look for differences between entities
        Institution institution =
            EntityConverter.createInstitution()
                .fromIHInstitution(ihInstitution)
                .withExisting(existing)
                .convert();

        InstitutionDiffResult.Builder diffBuilder = new InstitutionDiffResult.Builder();
        if (!institution.lenientEquals(existing)) {
          diffBuilder.newInstitution(institution).oldInstitution(existing);
        }

        // look for differences in staff
        diffBuilder.staffDiffResult(
            StaffDiffFinder.syncStaff(ihStaffSupplier.apply(ihInstitution.getCode()), institution));

        InstitutionDiffResult diff = diffBuilder.build();
        if (diff.isEmpty()) {
          syncResult.institutionNoChange(existing);
        } else {
          syncResult.institutionToUpdate(diff);
        }

      } else if (match.onlyOneCollectionMatch()) {
        Collection existing = match.collections.iterator().next();
        collectionsCopy.remove(existing);

        if (isIHOutdated(ihInstitution, existing.getModified())) {
          syncResult.collectionConflict(
              Issue.createOutdatedIHCollectionIssue(existing, ihInstitution));
          continue;
        }

        // we look for differences between entities
        Collection collection =
            EntityConverter.createCollection()
                .fromIHInstitution(ihInstitution)
                .withExisting(existing)
                .convert();

        CollectionDiffResult.Builder diffBuilder = new CollectionDiffResult.Builder();
        if (!collection.lenientEquals(existing)) {
          diffBuilder.newCollection(collection).oldCollection(existing);
        }

        // look for differences in staff
        diffBuilder.staffDiffResult(
            StaffDiffFinder.syncStaff(ihStaffSupplier.apply(ihInstitution.getCode()), collection));

        CollectionDiffResult diff = diffBuilder.build();
        if (diff.isEmpty()) {
          syncResult.collectionNoChange(existing);
        } else {
          syncResult.collectionToUpdate(diff);
        }

      } else if (match.noMatches()) {
        // create institution
        Institution institution =
            EntityConverter.createInstitution().fromIHInstitution(ihInstitution).convert();

        institution.setContacts(
            ihStaffSupplier.apply(ihInstitution.getCode()).stream()
                .map(s -> EntityConverter.createPerson().fromIHStaff(s).convert())
                .collect(Collectors.toList()));

        log.info("Creating new institution: {}", institution.getName());
        syncResult.institutionToCreate(institution);

      } else {
        // Conflict that needs resolved manually
        syncResult.conflict(Issue.createConflict(match.getAllMatches(), ihInstitution));
        log.info(
            "Conflict. {} institutions and {} collections are candidate matches in registry: ",
            ihInstitution.getOrganization());

        institutionsCopy.removeAll(match.institutions);
        collectionsCopy.removeAll(match.collections);
      }
    }

    return syncResult.build();
  }

  private static boolean isIHOutdated(IHInstitution ihInstitution, Date modified) {
    return modified.toInstant().isAfter(Instant.parse(ihInstitution.getDateModified()));
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
    CompletableFuture.allOf(matchedInstitutionsFuture, matchedCollectionsFuture);

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
