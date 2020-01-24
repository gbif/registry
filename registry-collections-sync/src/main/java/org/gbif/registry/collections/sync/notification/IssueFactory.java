package org.gbif.registry.collections.sync.notification;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.diff.DiffResult;
import org.gbif.registry.collections.sync.ih.IHEntity;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.ih.IHStaff;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import static org.gbif.registry.collections.sync.SyncConfig.NotificationConfig;

/** Factory to create {@link Issue}. */
public class IssueFactory {

  private static final String IH_OUTDATED_TITLE = "The IH %s with IRN %s is outdated";
  private static final String ENTITY_CONFLICT_TITLE =
      "IH %s with IRN %s matches with multiple GrSciColl entities";
  private static final String FAILS_TITLE =
      "Some operations have failed updating the registry in the IH sync";
  private static final String NEW_LINE = "\n";
  private static final String CODE_SEPARATOR = "```";
  private static final List<String> DEFAULT_LABELS = Collections.singletonList("GrSciColl-IH sync");

  private static final UnaryOperator<String> PORTAL_URL_NORMALIZER =
      url -> {
        if (url != null && url.endsWith("/")) {
          return url.substring(0, url.length() - 1);
        }
        return url;
      };

  private final NotificationConfig config;
  private final String ihInstitutionLink;
  private final String ihStaffLink;
  private final String registryInstitutionLink;
  private final String registryCollectionLink;
  private final String registryPersonLink;

  private IssueFactory(NotificationConfig config) {
    this.config = config;
    this.ihInstitutionLink =
        PORTAL_URL_NORMALIZER.apply(config.getIhPortalUrl()) + "/ih/herbarium-details/?irn=%s";
    this.ihStaffLink =
        PORTAL_URL_NORMALIZER.apply(config.getIhPortalUrl()) + "/ih/person-details/?irn=%s";
    this.registryInstitutionLink =
        PORTAL_URL_NORMALIZER.apply(config.getRegistryPortalUrl()) + "/institution/%s";
    this.registryCollectionLink =
        PORTAL_URL_NORMALIZER.apply(config.getRegistryPortalUrl()) + "/collection/%s";
    this.registryPersonLink =
        PORTAL_URL_NORMALIZER.apply(config.getRegistryPortalUrl()) + "/person/%s";
  }

  public static IssueFactory fromConfig(NotificationConfig config) {
    return new IssueFactory(config);
  }

  public static IssueFactory fromDefaults() {
    NotificationConfig config = new NotificationConfig();
    config.setGhIssuesAssignees(Collections.emptyList());
    return new IssueFactory(config);
  }

  public Issue createOutdatedIHStaffIssue(Person grSciCollPerson, IHStaff ihStaff) {
    return createOutdatedIHEntityIssue(
        grSciCollPerson, ihStaff.getIrn(), ihStaff.toString(), EntityType.IH_STAFF);
  }

  public <T extends IHEntity> Issue createOutdatedIHInstitutionIssue(
      CollectionEntity grSciCollEntity, T ihInstitution) {
    return createOutdatedIHEntityIssue(
        grSciCollEntity,
        ihInstitution.getIrn(),
        ihInstitution.toString(),
        EntityType.IH_INSTITUTION);
  }

  private Issue createOutdatedIHEntityIssue(
      CollectionEntity grSciCollEntity,
      String irn,
      String ihEntityAsString,
      EntityType entityType) {

    // create body
    StringBuilder body = new StringBuilder();
    body.append("The IH ")
        .append(createLink(irn, entityType))
        .append(":")
        .append(formatEntity(ihEntityAsString))
        .append("has a more up-to-date ")
        .append(
            createLink(grSciCollEntity.getKey().toString(), getRegistryEntityType(grSciCollEntity)))
        .append(" in GrSciColl:")
        .append(formatEntity(grSciCollEntity.toString()))
        .append("Please check which one should remain and sync them in both systems.");

    if (entityType != EntityType.IH_STAFF) {
      body.append(" Remember to sync the associated staff too.");
    }

    return Issue.builder()
        .title(String.format(IH_OUTDATED_TITLE, entityType.title, irn))
        .body(body.toString())
        .assignees(config.getGhIssuesAssignees())
        .labels(DEFAULT_LABELS)
        .build();
  }

  public Issue createConflict(List<CollectionEntity> entities, IHInstitution ihInstitution) {
    return createConflict(entities, ihInstitution, EntityType.IH_INSTITUTION);
  }

  public Issue createStaffConflict(List<Person> persons, IHStaff ihStaff) {
    return createConflict(persons, ihStaff, EntityType.IH_STAFF);
  }

  private <T extends CollectionEntity> Issue createConflict(
      List<T> entities, IHEntity ihEntity, EntityType ihEntityType) {
    // create body
    StringBuilder body = new StringBuilder();
    body.append("The IH ")
        .append(createLink(ihEntity.getIrn(), ihEntityType))
        .append(":")
        .append(formatEntity(ihEntity))
        .append("have multiple candidates in GrSciColl: " + NEW_LINE);
    entities.forEach(
        e ->
            body.append(createLink(e.getKey().toString(), getRegistryEntityType(e), true))
                .append(formatEntity(e)));
    body.append("A IH ")
        .append(ihEntityType.title)
        .append(" should be associated to only one GrSciColl entity. Please resolve the conflict.");

    return Issue.builder()
        .title(String.format(ENTITY_CONFLICT_TITLE, ihEntityType.title, ihEntity.getIrn()))
        .body(body.toString())
        .assignees(config.getGhIssuesAssignees())
        .labels(DEFAULT_LABELS)
        .build();
  }

  public Issue createFailsNotification(List<DiffResult.FailedAction> fails) {
    // create body
    StringBuilder body = new StringBuilder();
    body.append(
        "The next operations have failed when updating the registry with the IH data. Please check them: ");

    fails.forEach(
        f ->
            body.append(NEW_LINE)
                .append("Error: ")
                .append(f.getMessage())
                .append(NEW_LINE)
                .append("Entity: ")
                .append(f.getEntity()));

    return Issue.builder()
        .title(FAILS_TITLE)
        .body(body.toString())
        .assignees(config.getGhIssuesAssignees())
        .labels(DEFAULT_LABELS)
        .build();
  }

  private String formatEntity(Object entity) {
    return NEW_LINE
        + CODE_SEPARATOR
        + NEW_LINE
        + entity.toString()
        + NEW_LINE
        + CODE_SEPARATOR
        + NEW_LINE;
  }

  private String createLink(String id, EntityType entityType) {
    return createLink(id, entityType, false);
  }

  private String createLink(String id, EntityType entityType, boolean omitText) {
    String linkTemplate;
    if (entityType == EntityType.IH_INSTITUTION) {
      linkTemplate = ihInstitutionLink;
    } else if (entityType == EntityType.IH_STAFF) {
      linkTemplate = ihStaffLink;
    } else if (entityType == EntityType.INSTITUTION) {
      linkTemplate = registryInstitutionLink;
    } else if (entityType == EntityType.COLLECTION) {
      linkTemplate = registryCollectionLink;
    } else {
      linkTemplate = registryPersonLink;
    }

    URI uri = URI.create(String.format(linkTemplate, id));
    String text = omitText ? uri.toString() : entityType.title;

    return "[" + text + "](" + uri.toString() + ")";
  }

  private EntityType getRegistryEntityType(CollectionEntity entity) {
    if (entity instanceof Institution) {
      return EntityType.INSTITUTION;
    } else if (entity instanceof Collection) {
      return EntityType.COLLECTION;
    } else {
      return EntityType.PERSON;
    }
  }

  private enum EntityType {
    IH_INSTITUTION("institution"),
    IH_STAFF("staff"),
    INSTITUTION("institution"),
    COLLECTION("collection"),
    PERSON("person");

    String title;

    EntityType(String title) {
      this.title = title;
    }
  }
}
