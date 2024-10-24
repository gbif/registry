package org.gbif.registry.ws.it.collections.service.descriptors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gbif.registry.service.collections.descriptors.InterpretedResult;
import org.gbif.registry.service.collections.descriptors.Interpreter;
import org.gbif.registry.test.mocks.ConceptClientMock;
import org.gbif.vocabulary.client.ConceptClient;
import org.junit.jupiter.api.Test;

public class InterpreterIT {

  private static final ConceptClient conceptClient = new ConceptClientMock();

  @Test
  public void typeStatusInterpreterTest() {
    Map<String, String> valuesMap = new HashMap<>();
    valuesMap.put("dwc:typeStatus", "allotype|hyotype|foo|possibly foo");

    InterpretedResult<List<String>> result =
        Interpreter.interpretTypeStatus(valuesMap, conceptClient);
    assertEquals(2, result.getResult().size());
    assertEquals(2, result.getIssues().size());
  }
}
