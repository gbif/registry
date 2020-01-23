package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.*;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.EntityDiffResult;
import static org.gbif.registry.collections.sync.diff.Utils.encodeIRN;
import static org.gbif.registry.collections.sync.diff.Utils.isIHOutdated;
import static org.gbif.registry.collections.sync.diff.Utils.mapByIrn;

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

  private final List<IHInstitution> ihInstitutions;
  private final Function<String, List<IHStaff>> ihStaffFetcher;
  private final Map<String, Set<Institution>> institutionsByIrn;
  private final Map<String, Set<Collection>> collectionsByIrn;
  private final EntityConverter entityConverter;
  private final StaffDiffFinder staffDiffFinder;

  @Builder
  private IndexHerbariorumDiffFinder(
      List<IHInstitution> ihInstitutions,
      Function<String, List<IHStaff>> ihStaffFetcher,
      List<Institution> institutions,
      List<Collection> collections,
      List<Person> persons,
      EntityConverter entityConverter) {
    this.ihInstitutions = ihInstitutions;
    this.ihStaffFetcher = ihStaffFetcher;
    this.entityConverter = entityConverter;
    this.institutionsByIrn = mapByIrn(institutions);
    this.collectionsByIrn = mapByIrn(collections);
    this.staffDiffFinder =
        StaffDiffFinder.builder()
            .allGrSciCollPersons(persons)
            .entityConverter(entityConverter)
            .build();
  }

  public DiffResult find() {
    DiffResult.DiffResultBuilder diffResult = DiffResult.builder();

    for (IHInstitution ihInstitution : ihInstitutions) {

      // locate potential matches in GrSciColl
      Match match =
          findMatches(institutionsByIrn, collectionsByIrn, encodeIRN(ihInstitution.getIrn()));

      if (match.onlyOneInstitutionMatch()) {
        Institution existing = match.institutions.iterator().next();
        log.info("Institution {} matched with IH {}", existing.getKey(), ihInstitution.getCode());

        if (isIHOutdated(ihInstitution, existing)) {
          diffResult.outdatedInstitution(new DiffResult.IHOutdated<>(ihInstitution, existing));
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

        if (isIHOutdated(ihInstitution, existing)) {
          diffResult.outdatedInstitution(new DiffResult.IHOutdated<>(ihInstitution, existing));
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

        diffResult.conflict(new DiffResult.Conflict<>(ihInstitution, match.getAllMatches()));
      }
    }

    return diffResult.build();
  }

  private <T extends CollectionEntity & LenientEquals<T> & Contactable>
      EntityDiffResult<T> checkEntityDiff(IHInstitution ihInstitution, T newEntity, T existing) {

    EntityDiffResult.EntityDiffResultBuilder<T> updateDiffBuilder = EntityDiffResult.builder();
    if (!newEntity.lenientEquals(existing)) {
      updateDiffBuilder.newEntity(newEntity).oldEntity(existing);
    }

    // look for differences in staff
    log.info("Syncing staff for IH institution {}", ihInstitution.getCode());
    DiffResult.StaffDiffResult<T> staffDiffResult =
        staffDiffFinder.syncStaff(
            newEntity, ihStaffFetcher.apply(ihInstitution.getCode()), existing.getContacts());
    updateDiffBuilder.staffDiffResult(staffDiffResult);

    return updateDiffBuilder.build();
  }

  private Match findMatches(
      Map<String, Set<Institution>> institutions,
      Map<String, Set<Collection>> collections,
      String irn) {
    Match match = new Match();
    match.institutions = institutions.getOrDefault(irn, Collections.emptySet());
    match.collections = collections.getOrDefault(irn, Collections.emptySet());

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
