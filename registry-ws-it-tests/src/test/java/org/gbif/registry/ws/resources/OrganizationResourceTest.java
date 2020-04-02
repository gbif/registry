/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
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
package org.gbif.registry.ws.resources;

import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class OrganizationResourceTest {

  /**
   * Ensure randomly generated password has size greater than or equal to MINIMUM_PASSWORD_SIZE and
   * less than MAXIMUM_PASSWORD_SIZE, and that subsequent passwords being generated are in fact
   * unique.
   */
  @Test
  public void testGeneratePassword() {
    String password1 = OrganizationResource.generatePassword();
    assertTrue(password1.length() >= OrganizationResource.MINIMUM_PASSWORD_SIZE);
    assertTrue(password1.length() < OrganizationResource.MAXIMUM_PASSWORD_SIZE);

    String password2 = OrganizationResource.generatePassword();
    assertTrue(password2.length() >= OrganizationResource.MINIMUM_PASSWORD_SIZE);
    assertTrue(password2.length() < OrganizationResource.MAXIMUM_PASSWORD_SIZE);

    String password3 = OrganizationResource.generatePassword();
    assertTrue(password3.length() >= OrganizationResource.MINIMUM_PASSWORD_SIZE);
    assertTrue(password3.length() < OrganizationResource.MAXIMUM_PASSWORD_SIZE);

    String password4 = OrganizationResource.generatePassword();
    assertTrue(password4.length() >= OrganizationResource.MINIMUM_PASSWORD_SIZE);
    assertTrue(password4.length() < OrganizationResource.MAXIMUM_PASSWORD_SIZE);

    assertNotEquals(password1, password2);
    assertNotEquals(password1, password3);
    assertNotEquals(password1, password4);

    assertNotEquals(password2, password3);
    assertNotEquals(password2, password4);

    assertNotEquals(password3, password4);
  }
}
