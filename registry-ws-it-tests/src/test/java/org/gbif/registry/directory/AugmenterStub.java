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
package org.gbif.registry.directory;

import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Node;
import org.gbif.api.vocabulary.ContactType;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class AugmenterStub implements Augmenter {

  @Override
  public Node augment(Node node) {
    if (node != null) {
      node.setParticipantSince(2001);
      node.setAbbreviation("GBIF.ES");
      node.setCity("Madrid");
      node.setPostalCode("E-28014");
      node.setOrganization("Real Jardín Botánico - CSIC");
      for (int i = 1; i < 8; i++) {
        Contact c = new Contact();
        c.setKey(i);
        c.setLastName("Name " + i);
        c.setType(ContactType.NODE_STAFF);
        node.getContacts().add(c);
      }
    }
    return node;
  }
}
