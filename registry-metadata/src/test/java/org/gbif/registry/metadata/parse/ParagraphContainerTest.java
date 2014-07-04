package org.gbif.registry.metadata.parse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParagraphContainerTest {

  @Test
  public void testToString() throws Exception {
    ParagraphContainer container = new ParagraphContainer();

    assertNull(container.toString());

    container.appendParagraph("Hello");
    assertEquals("Hello", container.toString());

    container.appendParagraph("world!");
    assertEquals("Hello\nworld!", container.toString());
  }

}
