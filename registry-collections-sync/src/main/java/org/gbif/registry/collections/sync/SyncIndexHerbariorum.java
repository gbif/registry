package org.gbif.registry.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollClient;
import org.gbif.registry.collections.sync.ih.IhInstitution;
import org.gbif.registry.collections.sync.ih.IndexHerbariorumClient;
import org.gbif.registry.collections.sync.notification.GithubClient;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
 *
 * <p>TODO: Add synchronisation of staff
 */
@Slf4j
public class SyncIndexHerbariorum {

  public static void main(String[] args) throws IOException {

    log.info("Loading IH");
    // TODO: parametrize
    IndexHerbariorumClient indexHerbariorumClient =
        IndexHerbariorumClient.create("http://sweetgum.nybg.org/science/api/v1/");
    CompletableFuture<List<IhInstitution>> ihInstitutionsFuture =
        CompletableFuture.supplyAsync(indexHerbariorumClient::getInstitutions);

    // TODO: parametrize
    GrSciCollClient grSciCollClient =
        GrSciCollClient.create("http://api.gbif.org/v1/grscicoll/", "mlopezg", "P1p3r!!");

    log.info("Loading Institutions");
    CompletableFuture<List<Institution>> institutionsFuture =
        CompletableFuture.supplyAsync(grSciCollClient::getInstitutions);

    log.info("Loading Collections");
    CompletableFuture<List<Collection>> collectionsFuture =
        CompletableFuture.supplyAsync(grSciCollClient::getCollections);

    CompletableFuture.allOf(ihInstitutionsFuture, institutionsFuture, collectionsFuture);

    List<IhInstitution> herbaria = ihInstitutionsFuture.join();
    List<Institution> institutions = institutionsFuture.join();
    List<Collection> collections = collectionsFuture.join();

    Counter counter = new Counter();
    for (IhInstitution ihInstitution : herbaria) {
      counter.total++;

      // locate potential matches in GrSciColl
      Set<Institution> matchedInstitutions =
          filterById(institutions, encodeIRN(ihInstitution.getIrn()));
      Set<Collection> matchedCollections = filterById(collections, encodeIRN(ihInstitution.getIrn()));

      if (matchedInstitutions.size() == 1 && matchedCollections.isEmpty()) {
        Institution existing = matchedInstitutions.iterator().next();

        if (existing.getModified().toInstant().isAfter(Instant.parse(ihInstitution.getDateModified()))) {
         // TODO: create issue
          counter.conflict++;
          continue;
        }

        Institution institution =
            GrSciCollEntityConverter.createInstitution()
                .fromIHInstitution(ihInstitution)
                .withExisting(existing)
                .convert();
        if (!institution.lenientEquals(existing)) {
          counter.institutionUpdated++;
          // TODO: if calls to update or create fail put it in a try-catch and follow with the rest?? same for the others
          // TODO: update institution
          log.info("Updating institution: {}", institution.getName());
        } else {
          counter.institutionNoChange++;
          log.info("Skipping institution [no change]: {}", institution.getName());
        }

      } else if (matchedCollections.size() == 1 && matchedInstitutions.isEmpty()) {
        Collection existing = matchedCollections.iterator().next();

        if (existing.getModified().toInstant().isAfter(Instant.parse(ihInstitution.getDateModified()))) {
          // TODO: create issue
          counter.conflict++;
          continue;
        }

        Collection collection =
            GrSciCollEntityConverter.createCollection()
                .fromIHInstitution(ihInstitution)
                .withExisting(existing)
                .convert();
        if (!collection.lenientEquals(existing)) {
          counter.collectionUpdated++;
          // TODO: update collection
          log.info("Updating collection: {}", collection.getName());
        } else {
          counter.collectionNoChange++;
          log.info("Skipping collection [no change]: {}", collection.getName());
        }

      } else if (matchedInstitutions.isEmpty() && matchedCollections.isEmpty()) {
        counter.institutionCreated++;
        Institution institution =
            GrSciCollEntityConverter.createInstitution().fromIHInstitution(ihInstitution).convert();
        log.info("Creating new institution: {}", institution.getName());
        // TODO: create
        // remember to add the identifier!

      } else {
        counter.conflict++;
        // Conflict that needs resolved manually
        // TODO: create issue
        log.info(
            "Conflict. {} institutions and {} collections are candidate matches in registry: ",
            ihInstitution.getOrganization());
      }

      log.debug("{}: {}", counter.total, ihInstitution.getOrganization());
    }
    log.info("Synchronisation finished. {}", counter);
  }

  @ToString
  private static class Counter {
    int total;
    int institutionUpdated;
    int institutionCreated;
    int institutionNoChange;
    int collectionUpdated;
    int collectionNoChange;
    int conflict;
  }

  /**
   * Encodes the IH IRN into the format stored on the GRSciColl identifier. E.g. 123 ->
   * gbif:ih:irn:123
   */
  private static String encodeIRN(String irn) {
    return "gbif:ih:irn:" + irn;
  }

  /** Filters the source to only those having the given ID. */
  private static <T extends Identifiable> Set<T> filterById(List<T> source, String id) {
    return source.stream()
        .filter(
            o -> o.getIdentifiers().stream().anyMatch(i -> Objects.equals(id, i.getIdentifier())))
        .collect(Collectors.toSet());
  }
}
