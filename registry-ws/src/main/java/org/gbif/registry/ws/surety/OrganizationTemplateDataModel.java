package org.gbif.registry.ws.surety;

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.surety.email.BaseTemplateDataModel;

import java.net.URL;
import javax.annotation.Nullable;

/**
 * Specialized model that contains data to be used by the template to generate an email related to
 * {@link Organization}.
 *
 * This class is required to be public for Freemarker.
 */
public class OrganizationTemplateDataModel extends BaseTemplateDataModel {

  private final Organization organization;
  private final URL organizationUrl;
  private final Node endorsingNode;
  private final boolean reachableNodeManager;

  static OrganizationTemplateDataModel buildEndorsementModel(String name, URL url, Organization organization,
                                                             Node endorsingNode, boolean reachableNodeManager){
    return new OrganizationTemplateDataModel(name, url, organization, null, endorsingNode, reachableNodeManager);
  }

  static OrganizationTemplateDataModel buildEndorsedModel(String name, Organization organization, URL organizationUrl, Node endorsingNode){
    return new OrganizationTemplateDataModel(name, null, organization, organizationUrl, endorsingNode, false);
  }

  /**
   *
   * @param name
   * @param url
   * @param organization
   * @param organizationUrl
   * @param endorsingNode
   * @param reachableNodeManager
   */
  public OrganizationTemplateDataModel(String name, @Nullable URL url, Organization organization,
                                       @Nullable URL organizationUrl, Node endorsingNode, boolean reachableNodeManager) {
    super(name, url);
    this.organization = organization;
    this.organizationUrl = organizationUrl;
    this.endorsingNode = endorsingNode;
    this.reachableNodeManager = reachableNodeManager;
  }

  public Organization getOrganization() {
    return organization;
  }

  public URL getOrganizationUrl() {
    return organizationUrl;
  }

  public Node getEndorsingNode() {
    return endorsingNode;
  }

  public boolean hasReachableNodeManager() {
    return reachableNodeManager;
  }
}
