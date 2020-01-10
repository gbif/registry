package org.gbif.registry.collections.sync.notification;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Issue {

  private static final String IH_OUTDATED_TITLE = "The IH %s is outdated";
  private static final String STAFF_CONFLICT_TITLE =
      "Multiple GrSciColl persons associated to the same IH staff";
  private static final String NEW_LINE = "\n";

  // TODO: check what assignees to put
  private static final List<String> DEFAULT_ASSIGNEES = Arrays.asList("asturcon");
  // TODO: check what labels to put
  private static final List<String> DEFAULT_LABELS = Arrays.asList("GrSciColl-IH conflict");

  private String title;
  private String body;
  private List<String> asignees;
  private List<String> labels;

  public static Issue createOutdatedIHInstitutionIssue(
      Institution grSciCollInstitution, IHInstitution ihInstitution) {
    return createOutdatedIHEntityIssue(grSciCollInstitution, ihInstitution, "institution");
  }

  public static Issue createOutdatedIHCollectionIssue(
      Collection grSciCollCollection, IHInstitution ihInstitution) {
    return createOutdatedIHEntityIssue(grSciCollCollection, ihInstitution, "collection");
  }

  private static Issue createOutdatedIHEntityIssue(
      CollectionEntity grSciCollEntity, IHInstitution ihInstitution, final String entityType) {

    // create body
    StringBuilder body = new StringBuilder();
    body.append("The IH ")
        .append(entityType)
        .append(":")
        .append(NEW_LINE)
        .append(ihInstitution)
        .append(NEW_LINE)
        .append("has a more up-to-date entity in GrSciColl:")
        .append(NEW_LINE)
        .append(grSciCollEntity)
        .append(
            "Please check which one should remain and sync them in both systems. Remember to sync the associated staff too.");

    return new Issue(
        String.format(IH_OUTDATED_TITLE, entityType),
        body.toString(),
        DEFAULT_ASSIGNEES,
        DEFAULT_LABELS);
  }

  public static Issue createConflict(List<CollectionEntity> entities, IHInstitution ihInstitution) {
    // create body
    StringBuilder body = new StringBuilder();
    body.append("The IH institution: \n")
        .append(ihInstitution)
        .append("\n")
        .append("have multiple candidates in GrSciColl: \n");
    entities.forEach(e -> body.append(e).append("\n"));
    body.append(
        "A IH institution should be associated to only one GrSciColl entity. Please resolve the conflict.");

    return new Issue(STAFF_CONFLICT_TITLE, body.toString(), DEFAULT_ASSIGNEES, DEFAULT_LABELS);
  }

  public static Issue createMultipleStaffIssue(List<Person> persons, IHStaff ihStaff) {
    // create body
    StringBuilder body = new StringBuilder();
    body.append("The IH staff: \n")
        .append(ihStaff)
        .append("\n")
        .append("is associated to all the following GrSciColl persons: \n");
    persons.forEach(p -> body.append(p).append("\n"));
    body.append(
        "A IH staff should be associated to only one GrSciColl person. Please resolve the conflict.");

    return new Issue(STAFF_CONFLICT_TITLE, body.toString(), DEFAULT_ASSIGNEES, DEFAULT_LABELS);
  }
}
