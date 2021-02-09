package org.gbif.registry.ws.it.persistence.mapper;

import org.gbif.api.model.registry.MachineTag;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MachineTagMapperIT extends BaseItTest {

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
