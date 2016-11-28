package org.gbif.registry.metadata.parse;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to temporarily keep paragraph strings before they are used as a single,
 * concatenated string argument in other rules.
 * Digester needs public access to this otherwise package scoped class.
 * </br>
 * Note HTML is used to concatenate paragraphs using <p> instead of newline character ("\n"), see POR-3138.
 * @see <a href="http://dev.gbif.org/issues/browse/POR-3138">POR-3138</a>
 */
public class ParagraphContainer {

  private static Joiner PARA_JOIN = Joiner.on("\n");
  private List<String> paragraphs = Lists.newArrayList();

  public void appendParagraph(String para) {
    if (!Strings.isNullOrEmpty(para)) {
      String paragraph = para.trim();
      paragraph = StringUtils.prependIfMissing(paragraph, "<p>");
      paragraph = StringUtils.appendIfMissing(paragraph, "</p>");
      paragraphs.add(paragraph);
    }
  }

  public String toString() {
    if (paragraphs.isEmpty()) {
      return null;
    }
    return PARA_JOIN.join(paragraphs);
  }
}