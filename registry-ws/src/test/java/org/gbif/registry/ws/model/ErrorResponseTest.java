package org.gbif.registry.ws.model;

import java.io.ByteArrayOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;

/**
 *
 */
public class ErrorResponseTest {

  /**
   * Ensure we can serialize a ErrorResponse properly (POR-2993)
   */
  @Test
  public void testErrorResponse() throws JAXBException {
    ErrorResponse error = new ErrorResponse("error");
    JAXBContext jc = JAXBContext.newInstance(ErrorResponse.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Marshaller marshaller = jc.createMarshaller();
    marshaller.marshal(error, baos);
  }
}
