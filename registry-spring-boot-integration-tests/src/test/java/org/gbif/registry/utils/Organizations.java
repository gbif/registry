package org.gbif.registry.utils;

import org.codehaus.jackson.type.TypeReference;
import org.gbif.api.model.registry.Organization;

import java.util.UUID;

public class Organizations extends JsonBackedData<Organization> {

  private static final Organizations INSTANCE = new Organizations();

  protected Organizations() {
    super("data/organization.json", new TypeReference<Organization>() {});
  }

  public static Organization newInstance(UUID endorsingNodeKey) {
    Organization o = INSTANCE.newTypedInstance();
    o.setEndorsingNodeKey(endorsingNodeKey);
    o.setPassword("password");
    return o;
  }
}
