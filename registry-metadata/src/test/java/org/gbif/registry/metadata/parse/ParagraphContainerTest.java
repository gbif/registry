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
    assertEquals("<p>Hello</p>", container.toString());

    container.appendParagraph("world!");
    assertEquals("<p>Hello</p>\n<p>world!</p>", container.toString());

    //make sure we won't use <p> is there is already one
    container.appendParagraph(" <p>is it me</p>");
    assertEquals("<p>Hello</p>\n<p>world!</p>\n<p>is it me</p>", container.toString());
  }

}
