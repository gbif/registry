package org.gbif.registry.collections.sync.ih;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IHUtils {

  /**
   * Encodes the IH IRN into the format stored on the GRSciColl identifier. E.g. 123 ->
   * gbif:ih:irn:123
   */
  public static String encodeIRN(String irn) {
    return "gbif:ih:irn:" + irn;
  }

  public static boolean isIHOutdated(String ihModified, Date modified) {
    return modified != null
        && modified
            .toInstant()
            .isAfter(LocalDate.parse(ihModified).atStartOfDay().toInstant(ZoneOffset.UTC));
  }
}
