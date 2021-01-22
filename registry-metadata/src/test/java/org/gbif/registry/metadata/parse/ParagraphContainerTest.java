/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.metadata.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ParagraphContainerTest {

  @Test
  public void testToString() {
    ParagraphContainer container = new ParagraphContainer();

    assertNull(container.toString());

    container.appendParagraph("Hello");
    assertEquals("Hello", container.toString());

    container.appendParagraph("world!");
    assertEquals("<p>Hello</p>\n<p>world!</p>", container.toString());

    // make sure we won't use <p> is there is already one
    container.appendParagraph(" <p>is it me</p>");
    assertEquals("<p>Hello</p>\n<p>world!</p>\n<p>is it me</p>", container.toString());
  }
}
