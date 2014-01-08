package org.gbif.registry.metadata.parse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParagraphContainerTest {

  @Test
  public void testToString() throws Exception {
    ParagraphContainer container = new ParagraphContainer();
    container.appendParagraph("Hello");
    assertEquals("Hello", container.toString());

    container.appendParagraph("world!");
    assertEquals("Hello<br/>world!", container.toString());
  }
}
