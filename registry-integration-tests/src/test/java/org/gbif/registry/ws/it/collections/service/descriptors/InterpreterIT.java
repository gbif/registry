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
package org.gbif.registry.ws.it.collections.service.descriptors;

import org.gbif.registry.service.collections.descriptors.InterpretedResult;
import org.gbif.registry.service.collections.descriptors.Interpreter;
import org.gbif.registry.test.mocks.ConceptClientMock;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
