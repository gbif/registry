package org.gbif.registry.domain.ws.surety;

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;
import org.gbif.registry.domain.mail.BaseTemplateDataModel;

import javax.annotation.Nullable;
import java.net.URL;

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

  public static OrganizationTemplateDataModel buildEndorsementModel(String name, URL url, Organization organization,
                                                             Node endorsingNode, boolean reachableNodeManager){
    return new OrganizationTemplateDataModel(name, url, organization, null, endorsingNode, reachableNodeManager);
  }

  public static OrganizationTemplateDataModel buildEndorsedModel(String name, Organization organization, URL organizationUrl, Node endorsingNode){
    return new OrganizationTemplateDataModel(name, null, organization, organizationUrl, endorsingNode, false);
  }

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
