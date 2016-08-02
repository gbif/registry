package org.gbif.registry.metadata.parse;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Class to temporarily keep paragraph strings before they are used as a single,
 * concatenated string argument in other rules.
 * Digester needs public access to this otherwise package scoped class.
 * </br>
 * Note HTML is used to concatenate paragraphs using break tag </br> instead of newline character ("\n"), see POR-3138.
 * @see <a href="http://dev.gbif.org/issues/browse/POR-3138">POR-3138</a>
 */
public class ParagraphContainer {

  private static Joiner PARA_JOIN = Joiner.on("</br>");
  private List<String> paragraphs = Lists.newArrayList();

  public void appendParagraph(String para) {
    if (!Strings.isNullOrEmpty(para)) {
      paragraphs.add(para.trim());
    }
  }

  public String toString() {
    if (paragraphs.isEmpty()) {
      return null;
    }
    return PARA_JOIN.join(paragraphs);
  }
}