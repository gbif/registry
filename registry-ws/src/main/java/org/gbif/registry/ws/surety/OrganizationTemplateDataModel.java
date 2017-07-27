package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.surety.email.BaseTemplateDataModel;

import java.net.URL;
import javax.annotation.Nullable;

/**
 * Specialized model that contains data to be used by the templace to generate an email related to
 * {@link Organization}.
 *
 * This class is required to be public for Freemarker.
 */
public class OrganizationTemplateDataModel extends BaseTemplateDataModel {

  private final Organization organisation;
  private final Node endorsingNode;

  /**
   *
   * @param name
   * @param url
   * @param organisation
   * @param endorsingNode
   */
  public OrganizationTemplateDataModel(String name, @Nullable URL url, Organization organisation, Node endorsingNode) {
    super(name, url);
    this.organisation = organisation;
    this.endorsingNode = endorsingNode;
  }

  public Organization getOrganisation() {
    return organisation;
  }

  public Node getEndorsingNode() {
    return endorsingNode;
  }

}
