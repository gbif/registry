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
package org.gbif.registry.doi.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RegistryDoiUtilsTest {

  @Test
  public void testGetYear() {
    // given
    Instant instant = LocalDateTime.of(2014, 12, 12, 0, 0).toInstant(ZoneOffset.UTC);
    Date date = Date.from(instant);

    // when
    String actualNonNull = org.gbif.registry.doi.util.RegistryDoiUtils.getYear(date);
    String actualNull = org.gbif.registry.doi.util.RegistryDoiUtils.getYear(null);

    // then
    assertEquals("2014", actualNonNull);
    assertNull(actualNull);
  }

  @Test
  public void testFdate() {
    // given
    Instant instant = LocalDateTime.of(2014, 12, 12, 0, 0).toInstant(ZoneOffset.UTC);
    Date date = Date.from(instant);

    // when
    String actual = org.gbif.registry.doi.util.RegistryDoiUtils.fdate(date);

    // then
    assertEquals("2014-12-12", actual);
  }
}
