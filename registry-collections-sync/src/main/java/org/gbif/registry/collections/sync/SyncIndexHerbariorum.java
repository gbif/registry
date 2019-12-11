package org.gbif.registry.collections.sync;

import lombok.ToString;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A synchronization utility that will ensure GRSciColl is up to date with IndexHerbariorum.
 * This operates as follows:
 * <ul>
 *   <li>Retrieve all Herbaria from IndexHerbariorum</li>
 *   <li>For each entity locate the equivalent Institution or Collection in GRSciColl using the IH IRN</li>
 *   <li>If the entity exists and they differ, update GrSciColl</li>
 *   <li>If the entity does not exist, insert it as an institution and with an identifier holding the IH IRN</li>
 * </ul>
 * <p>A future version of this may allow editing of IH entities in GRSciColl. Under that scenario when entities differ
 * more complex logic is required, likely requiring notification to GRSciColl and IH staff to resolve the differences.
 * <p>TODO: Add synchronisation of staff
 */
public class SyncIndexHerbariorum {
  private static Logger LOG = LoggerFactory.getLogger(SyncIndexHerbariorum.class);
  
  public static void main(String[] args) throws IOException {

    LOG.info("Loading IH");
    List<IndexHerbariorum.Institution> herbaria = IndexHerbariorum.institutions();
    LOG.info("Loading Institutions");
    List<Institution> institutions = GRSciColl.institutions();
    LOG.info("Loading Collections");
    List<Collection> collections = GRSciColl.collections();

    Counter counter = new Counter();
    for (IndexHerbariorum.Institution herbarium : herbaria) {
      counter.total++;

      // locate potential matches in GrSciColl
      Set<Institution> matchedInstitutions = filterById(institutions, encodeIRN(herbarium.getIrn()));
      Set<Collection> matchedCollections = filterById(collections, encodeIRN(herbarium.getIrn()));

      if (matchedInstitutions.size()==1 && matchedCollections.isEmpty()) {
        Institution existing = matchedInstitutions.iterator().next();
        Institution institution = buildInstitution(herbarium);
        if (!institution.lenientEquals(existing)) {
          counter.institutionUpdated++;
          LOG.info("Updating institution: {}", institution.getName());
        } else {
          counter.institutionNoChange++;
          LOG.info("Skipping institution [no change]: {}", institution.getName());
        }

      } else if (matchedCollections.size()==1 && matchedInstitutions.isEmpty()) {
        Collection existing = matchedCollections.iterator().next();
        Collection collection = buildCollection(herbarium, existing.getInstitutionKey());
        if (!collection.lenientEquals(existing)) {
          counter.collectionUpdated++;
          LOG.info("Updating collection: {}", collection.getName());
        } else {
          counter.collectionNoChange++;
          LOG.info("Skipping collection [no change]: {}", collection.getName());
        }


      } else if (matchedInstitutions.isEmpty() && matchedCollections.isEmpty()) {
        counter.institutionCreated++;
        Institution institution = buildInstitution(herbarium);
        LOG.info("Creating new institution: {}", institution.getName());
        // remember to add the identifier!

      } else {
        counter.conflict++;
        // Conflict that needs resolved manually
        LOG.info("Conflict. {} institutions and {} collections are candidate matches in registry: ",
          herbarium.getOrganization());

      }

      LOG.debug("{}: {}", counter.total, herbarium.getOrganization());
    }
    LOG.info("Synchronisation finished. {}", counter);
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
   * Creates an institution from the IH object.
   */
  private static Institution buildInstitution(IndexHerbariorum.Institution ih) {
    Institution i = new Institution();
    i.setName(ih.getOrganization());
    // TODO
    return i;
  }

  /**
   * Creates a collection from the IH object.
   */
  private static Collection buildCollection(IndexHerbariorum.Institution ih, UUID institutionKey) {
    Collection c = new Collection();
    c.setInstitutionKey(institutionKey);
    c.setName(ih.getOrganization());
    // TODO
    return c;
  }

  /**
   * Encodes the IH IRN into the format stored on the GRSciColl identifier.
   * E.g. 123 -> gbif:ih:irn:123
   */
  private static String encodeIRN(String irn) {
    return "gbif:ih:irn:" + irn;
  }

  /**
   * Filters the source to only those having the given ID.
   */
  private static <T extends Identifiable> Set<T> filterById(List<T> source, String id) {
    return source.stream()
      .filter(
        o ->
          o.getIdentifiers().stream()
            .filter(i -> Objects.equals(id, i.getIdentifier()))
            .count()
            > 0)
      .collect(Collectors.toSet());
  }

}
