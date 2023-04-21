package org.gbif.registry.service.collections.lookup.matchers;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MatchersTest {

  @Test
  public void parseUUIDTest() {
    assertEquals(
        UUID.fromString("685298f2-0bc1-4e44-b2d5-d9760574644e"),
        BaseMatcher.parseUUID(
            "https://www.gbif.org/grscicoll/collection/685298f2-0bc1-4e44-b2d5-d9760574644e"));
    assertEquals(
        UUID.fromString("685298f2-0bc1-4e44-b2d5-d9760574644e"),
        BaseMatcher.parseUUID(
            "https://www.gbif.org/grscicoll/collection/685298f2-0bc1-4e44-b2d5-d9760574644e/"));
    assertNull(
        BaseMatcher.parseUUID(
            "https://www.gbif.org/grscicoll/collection/685298f2-bc1-4e44-b2d5-d9760574644e"));
  }
}
