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
package org.gbif.registry.ws.model;

import org.gbif.registry.domain.ws.LegacyDataset;

import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Test;

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
