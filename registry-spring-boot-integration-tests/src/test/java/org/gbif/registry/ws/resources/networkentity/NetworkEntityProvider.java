package org.gbif.registry.ws.resources.networkentity;

import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.utils.Nodes;
import org.gbif.registry.utils.Organizations;

import java.util.UUID;

public class NetworkEntityProvider {

  public static NetworkEntity prepare(String entityType, UUID nodeKey, String title) {
    if (entityType.equals("organization")) {
      Organization organization = Organizations.newInstance(nodeKey);
      organization.setTitle(title);
      return organization;
    } else if (entityType.equals("node")) {
      return Nodes.newInstance();
    }

    throw new UnsupportedOperationException("not implemented");
  }
}
