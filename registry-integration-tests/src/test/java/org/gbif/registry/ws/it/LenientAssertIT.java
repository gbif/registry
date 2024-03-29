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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.Contact;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.test.TestDataFactory;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;

@Execution(ExecutionMode.CONCURRENT)
public class LenientAssertIT extends BaseItTest {

  private final TestDataFactory testDataFactory;

  @Autowired
  public LenientAssertIT(
      TestDataFactory testDataFactory,
      @Nullable SimplePrincipalProvider simplePrincipalProvider,
      EsManageServer esServer) {
    super(simplePrincipalProvider, esServer);
    this.testDataFactory = testDataFactory;
  }

  @Test
  public void testLenientAssert() {
    Contact c = testDataFactory.newContact();
    assertLenientEquals("Same object should be the same", c, c);
    Contact c2 = testDataFactory.newContact();
    assertLenientEquals("Equivalent object should be the same", c, c2);
    c2.setKey(1);
    assertLenientEquals("Key should be ignored", c, c2);
    c2.setCity("should fail");
    try {
      assertLenientEquals("Different objects should through assertion error", c, c2);
    } catch (AssertionError e) {
      // expected
    }
  }
}
