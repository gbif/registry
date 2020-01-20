package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.LenientEquals;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.util.*;
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

  public DiffResult find() {
    DiffResult.DiffResultBuilder diffResult = DiffResult.builder();

    // map the GrSciColl entities by their IH IRN
    Map<String, List<Institution>> institutionsByIrn = mapByIrn(institutions);
    Map<String, List<Collection>> collectionsByIrn = mapByIrn(collections);

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

  private <T extends CollectionEntity & Identifiable> Map<String, List<T>> mapByIrn(
      List<T> entities) {
    Map<String, List<T>> mapByIrn = new HashMap<>();
    entities.forEach(
        o ->
            o.getIdentifiers().stream()
                // TODO: use the enum when deployed
                                .filter(i -> i.getIdentifier().startsWith("gbif:ih:irn:"))
//                .filter(i -> i.getType() == IdentifierType.IH_IRN)
                .forEach(
                    i ->
                        mapByIrn
                            .computeIfAbsent(i.getIdentifier(), s -> new ArrayList<>())
                            .add(o)));
    return mapByIrn;
  }

  private <T extends CollectionEntity & LenientEquals<T> & Contactable>
      EntityDiffResult<T> checkEntityDiff(IHInstitution ihInstitution, T newEntity, T existing) {

    EntityDiffResult.EntityDiffResultBuilder<T> updateDiffBuilder = EntityDiffResult.builder();
    if (!newEntity.lenientEquals(existing)) {
      updateDiffBuilder.newEntity(newEntity).oldEntity(existing);
    }

    // look for differences in staff
    log.info("Syncing staff for IH institution {}", ihInstitution.getCode());
    DiffResult.StaffDiffResult staffDiffResult =
        StaffDiffFinder.syncStaff(
            ihStaffFetcher.apply(ihInstitution.getCode()), existing.getContacts(), entityConverter);
    updateDiffBuilder.staffDiffResult(staffDiffResult);

    return updateDiffBuilder.build();
  }

  private Match findMatches(
      Map<String, List<Institution>> institutions,
      Map<String, List<Collection>> collections,
      String irn) {
    Match match = new Match();
    List<Institution> institutionsMatched = institutions.get(irn);
    List<Collection> collectionsMatched = collections.get(irn);
    match.institutions =
        institutionsMatched != null ? new HashSet<>(institutionsMatched) : Collections.emptySet();
    match.collections =
        collectionsMatched != null ? new HashSet<>(collectionsMatched) : Collections.emptySet();

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
