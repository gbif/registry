package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;
import org.gbif.registry.collections.sync.notification.IssueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.EntityDiffResult;
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
@Builder
public class IndexHerbariorumDiffFinder {

  @NonNull private List<IHInstitution> ihInstitutions;
  @NonNull private Function<String, List<IHStaff>> ihStaffFetcher;
  private List<Institution> institutions;
  private List<Collection> collections;
  @NonNull private EntityConverter entityConverter;
  @NonNull private IssueFactory issueFactory;

  public DiffResult find() {
    DiffResult.DiffResultBuilder diffResult = DiffResult.builder();
    List<Institution> institutionsCopy =
        institutions != null ? new ArrayList<>(institutions) : new ArrayList<>();
    List<Collection> collectionsCopy =
        collections != null ? new ArrayList<>(collections) : new ArrayList<>();

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
              issueFactory.createOutdatedIHInstitutionIssue(existing, ihInstitution));
          continue;
        }

        // we look for differences between entities
        Institution institution = entityConverter.convertToInstitution(ihInstitution, existing);
        EntityDiffResult<Institution> entityDiff =
            checkEntityDiff(ihInstitution, institution, existing);

        if (entityDiff.isEmpty()) {
          diffResult.institutionNoChange(existing);
        } else {
          diffResult.institutionToUpdate(entityDiff);
        }
      } else if (match.onlyOneCollectionMatch()) {
        Collection existing = match.collections.iterator().next();
        log.info("Collection {} matched with IH {}", existing.getKey(), ihInstitution.getCode());
        collectionsCopy.remove(existing);

        if (isIHOutdated(ihInstitution.getDateModified(), existing)) {
          diffResult.collectionConflict(
              issueFactory.createOutdatedIHInstitutionIssue(existing, ihInstitution));
          continue;
        }

        // we look for differences between entities
        Collection collection = entityConverter.convertToCollection(ihInstitution, existing);
        EntityDiffResult<Collection> entityDiff =
            checkEntityDiff(ihInstitution, collection, existing);

        if (entityDiff.isEmpty()) {
          diffResult.collectionNoChange(existing);
        } else {
          diffResult.collectionToUpdate(entityDiff);
        }
      } else if (match.noMatches()) {
        log.info("New institution to create for IH: {}", ihInstitution.getCode());
        // create institution
        Institution institution = entityConverter.convertToInstitution(ihInstitution);

        institution.setContacts(
            ihStaffFetcher.apply(ihInstitution.getCode()).stream()
                .map(entityConverter::convertToPerson)
                .collect(Collectors.toList()));

        diffResult.institutionToCreate(institution);

      } else {
        // Conflict that needs resolved manually
        log.info(
            "Conflict. {} institutions and {} collections are candidate matches in registry for {}: ",
            match.institutions,
            match.collections,
            ihInstitution.getOrganization());

        diffResult.conflict(issueFactory.createConflict(match.getAllMatches(), ihInstitution));
        institutionsCopy.removeAll(match.institutions);
        collectionsCopy.removeAll(match.collections);
      }
    }

    return diffResult.build();
  }

  /** Filters the source to only those having the given ID. */
  private <T extends Identifiable> Set<T> filterById(List<T> source, String id) {
    return source.stream()
        .filter(
            o -> o.getIdentifiers().stream().anyMatch(i -> Objects.equals(id, i.getIdentifier())))
        .collect(Collectors.toSet());
  }

  private <T extends CollectionEntity & LenientEquals<T> & Contactable>
      EntityDiffResult<T> checkEntityDiff(IHInstitution ihInstitution, T newEntity, T existing) {

    EntityDiffResult.EntityDiffResultBuilder<T> updateDiffBuilder = EntityDiffResult.builder();
    if (!newEntity.lenientEquals(existing)) {
      updateDiffBuilder.newEntity(newEntity).oldEntity(existing);
    }

    // look for differences in staff
    log.info("Syncing staff for IH institution {}", ihInstitution.getCode());
    updateDiffBuilder.staffDiffResult(
        StaffDiffFinder.syncStaff(
            ihStaffFetcher.apply(ihInstitution.getCode()),
            existing.getContacts(),
            entityConverter,
            issueFactory));

    return updateDiffBuilder.build();
  }

  private Match findMatches(
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
