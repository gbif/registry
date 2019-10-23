package org.gbif.registry.ws.resources.networkentity;

import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.registry.utils.Installations;
import org.gbif.registry.utils.Networks;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;

import java.util.UUID;

public class NetworkEntityProvider {

  public static NetworkEntity prepare(String entityType, UUID referenceKey) {
    switch (entityType) {
      case "organization":
        return Organizations.newInstance(referenceKey);
      case "node":
        return Nodes.newInstance();
      case "dataset":
        throw new IllegalArgumentException("Entity dataset not supported");
      case "installation":
        return Installations.newInstance(referenceKey);
      case "network":
        return Networks.newInstance();
      default:
        throw new IllegalArgumentException("Entity " + entityType + " not supported");
    }
  }
}
