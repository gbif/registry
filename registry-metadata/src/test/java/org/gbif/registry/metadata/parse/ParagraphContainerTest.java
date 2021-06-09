/*
 * Copyright 2020-2021 Global Biodiversity Information Facility (GBIF)
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

  @Test
  public void testFromString() {
    ParagraphContainer container = new ParagraphContainer(null);
    assertEquals(0, container.getParagraphs().size());

    container = new ParagraphContainer("");
    assertEquals(0, container.getParagraphs().size());

    container = new ParagraphContainer("<p></p>");
    assertEquals(0, container.getParagraphs().size());

    container = new ParagraphContainer("Hello");
    assertEquals(1, container.getParagraphs().size());
    assertEquals("Hello", container.getParagraphs().get(0));

    container = new ParagraphContainer("<p>Hello</p>\n<p>world!</p>");
    assertEquals(2, container.getParagraphs().size());
    assertEquals("Hello", container.getParagraphs().get(0));
    assertEquals("world!", container.getParagraphs().get(1));

    container = new ParagraphContainer("<p>Hello</p>\n<p>world!</p>\n<p>is it me</p>");
    assertEquals(3, container.getParagraphs().size());
    assertEquals("Hello", container.getParagraphs().get(0));
    assertEquals("world!", container.getParagraphs().get(1));
    assertEquals("is it me", container.getParagraphs().get(2));

    // Simulate e.g. manual edits to the description
    container = new ParagraphContainer("\n<p>Hello</p><p>world!</p>\n<p>is it me</p>\n");
    assertEquals(3, container.getParagraphs().size());
    assertEquals("Hello", container.getParagraphs().get(0));
    assertEquals("world!", container.getParagraphs().get(1));
    assertEquals("is it me", container.getParagraphs().get(2));
  }
}
