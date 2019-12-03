package org.gbif.registry.ws.resources.networkentity;

import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.utils.Datasets;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Networks;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkEntityProvider {

  static final Map<String, Class<? extends NetworkEntity>> ENTITIES = new HashMap<>();

  static {
    ENTITIES.put("node", Node.class);
    ENTITIES.put("organization", Organization.class);
    ENTITIES.put("installation", Installation.class);
    ENTITIES.put("dataset", Dataset.class);
    ENTITIES.put("network", Network.class);
  }

  static NetworkEntity prepare(String entityType, UUID endorsingNodeKey, UUID organizationKey, UUID installationKey) {
    switch (entityType) {
      case "organization":
        return Organizations.newInstance(endorsingNodeKey);
      case "node":
        return Nodes.newInstance();
      case "dataset":
        return Datasets.newInstance(organizationKey, installationKey);
      case "installation":
        return Installations.newInstance(organizationKey);
      case "network":
        return Networks.newInstance();
      default:
        throw new IllegalArgumentException("Entity " + entityType + " not supported");
    }
  }
}
