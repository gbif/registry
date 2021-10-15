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
package org.gbif.registry.search.dataset.indexing;

import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple metadata sax base handler that collects all character data inside elements into a string
 * buffer, resetting the buffer with every element start and storing the string version of the
 * buffer in this.content when the end of the element is reached.
 */
public class FullTextSaxHandler extends DefaultHandler {

  private StringBuffer content;
  private Pattern normWhitespace = Pattern.compile("[\\s\n\r\t]+");

  @Override
  public void startDocument() throws SAXException {
    super.startDocument();
    content = new StringBuffer(" ");
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    content.append(ch, start, length);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes)
      throws SAXException {
    // as we remove tags we add whitespace to delimit words
    content.append(" ");
    // add attribute content to the full text
    for (int idx = attributes.getLength(); idx > 0; idx--) {
      content.append(attributes.getValue(idx - 1));
      content.append(" ");
    }
  }

  public String getFullText() {
    return normWhitespace.matcher(content.toString()).replaceAll(" ").trim();
  }
}
