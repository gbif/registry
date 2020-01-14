package org.gbif.registry.collections.sync.ih;

import lombok.Data;

/** Models the Index Herbariorum metadata that is used in the WS responses. */
@Data
public class IHMetadata {
  private int hits;
  private int code;
  private String message;
}
