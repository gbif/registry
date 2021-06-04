package org.gbif.registry.events.collections;

public enum EventType {
  CREATE,
  UPDATE,
  DELETE,
  LINK,
  UNLINK,
  REPLACE,
  CONVERSION_TO_COLLECTION,
  APPLY_SUGGESTION,
  DISCARD_SUGGESTION;
}
