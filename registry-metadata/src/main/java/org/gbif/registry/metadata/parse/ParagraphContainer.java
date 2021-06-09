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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

/**
 * Class to temporarily keep paragraph strings before they are used as a single, concatenated string
 * argument in other rules, and to reverse this transformation.
 * <br>
 * Digester needs public access to this otherwise package scoped class.
 * <br>
 * Note HTML is used to concatenate paragraphs using &lt;p/&gt; instead of newline character ("\n"), see POR-3138.
 *
 * @see <a href="http://dev.gbif.org/issues/browse/POR-3138">POR-3138</a>
 */
public class ParagraphContainer {

  private static final Joiner paraJoin = Joiner.on("\n");
  private final List<String> paragraphs = new ArrayList<>();

  public ParagraphContainer() {}

  public ParagraphContainer(String concatenated) {
    if (concatenated != null) {
      for (String c : concatenated.split("</p>\n?<p>")) {
        appendParagraph(c.replace("<p>", "").replace("</p>", ""));
      }
    }
  }

  public void appendParagraph(String para) {
    if (!Strings.isNullOrEmpty(para)) {
      paragraphs.add(para.trim());
    }
  }

  public List<String> getParagraphs() {
    return paragraphs;
  }

  @Override
  public String toString() {
    if (paragraphs.isEmpty()) {
      return null;
    }

    // do not wrap in HTML if we only have one element
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
