package org.gbif.registry.metadata.parse;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Class to temporarily keep paragraph strings before they are used as a single,
 * concatenated string argument in other rules.
 * Digester needs public access to this otherwise package scoped class.
 * </br>
 * Note HTML is used to concatenate paragraphs using <p> instead of newline character ("\n"), see POR-3138.
 *
 * @see <a href="http://dev.gbif.org/issues/browse/POR-3138">POR-3138</a>
 */
public class ParagraphContainer {

  private static Joiner paraJoin = Joiner.on("\n");
  private List<String> paragraphs = Lists.newArrayList();

  public void appendParagraph(String para) {
    if (!Strings.isNullOrEmpty(para)) {
      paragraphs.add(para.trim());
    }
  }

  @Override
  public String toString() {
    if (paragraphs.isEmpty()) {
      return null;
    }

    //do not wrap in HTML if we only have one element
    if (paragraphs.size() == 1) {
      return paragraphs.get(0);
    }

    // replace with StringJoiner when we move to Java 8
    List<String> wrappedParagraphs = Lists.newArrayList();
    for (String para : paragraphs) {
      wrappedParagraphs.add(wrapInHtmlParagraph(para));
    }
    return paraJoin.join(wrappedParagraphs);
  }

  private String wrapInHtmlParagraph(String para) {
    String paragraph = StringUtils.prependIfMissing(para, "<p>");
    paragraph = StringUtils.appendIfMissing(paragraph, "</p>");
    return paragraph;
  }
}
