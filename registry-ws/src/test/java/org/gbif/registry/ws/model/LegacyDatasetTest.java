package org.gbif.registry.ws.model;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;

import static org.junit.Assert.*;

public class LegacyDatasetTest {

  /**
   * Added since the DOI property in the Dataset class broke IT due to missing no arg constructor.
   */
  @Test
  public void testSerDe() throws JAXBException {
    LegacyDataset dataset = new LegacyDataset();
    dataset.setTitle("Test");
    JAXBContext jc = JAXBContext.newInstance(LegacyDataset.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Marshaller marshaller = jc.createMarshaller();
    marshaller.marshal(dataset, baos);
  }
}
