/*
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
package org.gbif.registry.domain.mail;

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;

import java.net.URL;

import javax.annotation.Nullable;

/**
 * Specialized model that contains data to be used by the template to generate an email related to
 * {@link Organization}.
 *
 * <p>This class is required to be public for Freemarker.
 */
public class OrganizationTemplateDataModel extends ConfirmableTemplateDataModel {

  private final Organization organization;
  private final URL organizationUrl;
  private final Node endorsingNode;
  private final boolean reachableNodeManager;

  public static OrganizationTemplateDataModel buildEndorsementModel(
      String name,
      URL url,
      Organization organization,
      Node endorsingNode,
      boolean reachableNodeManager) {
    return new OrganizationTemplateDataModel(
        name, url, organization, null, endorsingNode, reachableNodeManager);
  }

  public static OrganizationTemplateDataModel buildEndorsedModel(
      String name, Organization organization, URL organizationUrl, Node endorsingNode) {
    return new OrganizationTemplateDataModel(
        name, null, organization, organizationUrl, endorsingNode, false);
  }

  public OrganizationTemplateDataModel(
      String name,
      @Nullable URL url,
      Organization organization,
      @Nullable URL organizationUrl,
      Node endorsingNode,
      boolean reachableNodeManager) {
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
