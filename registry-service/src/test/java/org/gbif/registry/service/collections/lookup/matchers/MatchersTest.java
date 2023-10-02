/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    assertNull(
      BaseMatcher.parseUUID(
        "https://scientific-collections.gbif.org/institution/685298f2-bc1-4e44-b2d5-d9760574644e"));
  }
}
