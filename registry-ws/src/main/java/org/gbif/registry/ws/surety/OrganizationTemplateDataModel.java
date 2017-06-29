package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.surety.email.BaseTemplateDataModel;

import java.net.URL;

/**
 * Specialized model for Organizations.
 */
public class OrganizationTemplateDataModel extends BaseTemplateDataModel {

  private final Organization newOrganisation;
  private final Node endorsingNode;

  public OrganizationTemplateDataModel(String name, URL url, Organization newOrganisation, Node endorsingNode) {
    super(name, url);
    this.newOrganisation = newOrganisation;
    this.endorsingNode = endorsingNode;
  }

  public Organization getNewOrganisation() {
    return newOrganisation;
  }

  public Node getEndorsingNode() {
    return endorsingNode;
  }

}
