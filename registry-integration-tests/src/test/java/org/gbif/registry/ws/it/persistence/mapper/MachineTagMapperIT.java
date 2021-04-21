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
package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.registry.MachineTag;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MachineTagMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer("machine_tag");

  private final MachineTagMapper mapper;

  @Autowired
  public MachineTagMapperIT(
      MachineTagMapper mapper, SimplePrincipalProvider principalProvider, EsManageServer esServer) {
    super(principalProvider, esServer);
    this.mapper = mapper;
  }

  @Test
  public void testCreateAndGet() {
    MachineTag machineTag = new MachineTag();
    machineTag.setCreatedBy("mpodolskiy");
    machineTag.setName("tagName");
    machineTag.setNamespace("test-namespace.gbif.org");
    machineTag.setValue("tagValue");

    int machineTagKey = mapper.createMachineTag(machineTag);

    MachineTag machineTagStored = mapper.get(machineTagKey);
    assertTrue(machineTag.lenientEquals(machineTagStored));
  }
}
