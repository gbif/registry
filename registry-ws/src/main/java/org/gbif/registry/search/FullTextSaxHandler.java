package org.gbif.registry.search;

import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simple metadata sax base handler that collects all character data inside elements into a string buffer, resetting
 * the buffer with every element start and storing the string version of the buffer in this.content when the end of the
 * element is reached.
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
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
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