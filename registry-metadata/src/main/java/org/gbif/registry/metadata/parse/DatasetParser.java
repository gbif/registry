package org.gbif.registry.metadata.parse;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.vocabulary.MetadataType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.digester3.Digester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Main parser of dataset metadata that uses parser specific digester RuleSets for EML or Dublin Core.
 * It can automatically detect the document type if it is unknown or be used only with a specific parser type.
 *
 * This parser and its digester rules use the DatasetDelegator class to wrap a dataset and set complex bean components.
 */
public class DatasetParser {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetParser.class);

  private static class ParserDetectionHandler extends DefaultHandler {
    private static final String DC_NAMESPACE = "http://purl.org/dc/terms/";
    private MetadataType parserType;
    private LinkedList<String> path = Lists.newLinkedList();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      // look for EML
      if (path.size() == 1 && path.get(0).equals("eml") && localName.equals("dataset")) {
        parserType = MetadataType.EML;
      }

      // look for DC title
      if (parserType == null && DC_NAMESPACE.equals(uri)) {
        parserType = MetadataType.DC;
      }

      path.add(localName);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      String last = path.removeLast();
      if (!last.equals(localName)) {
        LOG.warn("XML path broken. Got {} but path stack gave {}", localName, last);
      }
    }
  }

  /**
   * @return the detected parser type or null
   * @throws java.lang.IllegalArgumentException in case no parser exists for this document
   */
  public static MetadataType detectParserType(InputStream xml) {
    try {
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      ParserDetectionHandler handler = new ParserDetectionHandler();
      xmlReader.setContentHandler(handler);
      InputSource inputSource = new InputSource(xml);
      xmlReader.parse(inputSource);
      if (handler.parserType != null) {
        return handler.parserType;
      }

    } catch (SAXException e) {
      LOG.error("Failed to SAX parse a document for parser type detection", e);
    } catch (IOException e) {
      LOG.warn("Failed to read metadata document for parser type detection", e);
    }
    throw new IllegalArgumentException("No parser found for this metadata document. Only EML or DC supported");
  }


  /**
   * Build from stream on-top of a preexisting Dataset populating its fields from a source metadata that's parsed.
   *
   * @param xml to read
   * @return The Dataset populated, never null
   * @throws java.io.IOException If the Stream cannot be read from
   * @throws IllegalArgumentException If the XML is not well formed or is not understood
   */
  public static Dataset build(InputStream xml) throws IOException {
    // buffer entire stream first. We need it several times
    final byte[] data = ByteStreams.toByteArray(xml);
    // detect the parser type
    return parse(detectParserType(new ByteArrayInputStream(data)), new ByteArrayInputStream(data));
  }

  public static Dataset parse(MetadataType type, InputStream xml) throws IOException {
    Digester digester = new Digester();
    digester.setNamespaceAware(true);

    // add digester rules based on parser type
    switch (type) {
      case EML:
        LOG.debug("Parsing EML document");
        digester.addRuleSet(new EMLRuleSet());
        break;
      case DC:
        LOG.debug("Parsing DC document");
        digester.addRuleSet(new DublinCoreRuleSet());
        break;
    }

    // push the Delegating object onto the stack
    DatasetWrapper delegator = new DatasetWrapper();
    digester.push(delegator);

    // now parse and return the dataset
    try {
        digester.parse(xml);
    } catch (ConversionException e) {
      // swallow
    } catch (SAXException e) {
      if (e.getException() == null || !e.getException().getClass().equals(ConversionException.class)) {
        // allow type conversions to happen
        throw new IllegalArgumentException("Invalid metadata xml document", e);
      }
    } finally {
      delegator.postProcess();
      Closeables.closeQuietly(xml);
    }

    return delegator.getTarget();
  }

}
