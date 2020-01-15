package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.CollectionEntity;

import java.time.LocalDate;
import java.time.ZoneOffset;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utils {

  private static final String GRSCICOLL_MIGRATION_USER = "registry-migration-grbio.gbif.org";

  /**
   * Encodes the IH IRN into the format stored on the GRSciColl identifier. E.g. 123 ->
   * gbif:ih:irn:123
   */
  public static String encodeIRN(String irn) {
    return "gbif:ih:irn:" + irn;
  }

  /**
   * Checks if a {@link CollectionEntity} is more up to date than a IH entity based in the modified
   * date. We don't take into account the GrSciColl modifications made during the initial migration.
   *
   * @param ihModifiedDate
   * @param grSciCollEntity
   * @return
   */
  public static boolean isIHOutdated(String ihModifiedDate, CollectionEntity grSciCollEntity) {
    return grSciCollEntity != null
        && grSciCollEntity.getModified() != null
        && !GRSCICOLL_MIGRATION_USER.equals(grSciCollEntity.getModifiedBy())
        && grSciCollEntity
            .getModified()
            .toInstant()
            .isAfter(LocalDate.parse(ihModifiedDate).atStartOfDay().toInstant(ZoneOffset.UTC));
  }
}
