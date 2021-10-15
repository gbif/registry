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
package org.gbif.registry.test;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.Organization;

import java.util.UUID;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TestDataFactory {

  private final Nodes nodes;
  private final Organizations organizations;
  private final Identifiers identifiers;
  private final Endpoints endpoints;
  private final MachineTags machineTags;
  private final Comments comments;
  private final Datasets datasets;
  private final Installations installations;
  private final Contacts contacts;
  private final Networks networks;

  @Autowired
  public TestDataFactory(
      Nodes nodes,
      Organizations organizations,
      Identifiers identifiers,
      Endpoints endpoints,
      MachineTags machineTags,
      Comments comments,
      Datasets datasets,
      Installations installations,
      Contacts contacts,
      Networks networks) {
    this.nodes = nodes;
    this.organizations = organizations;
    this.identifiers = identifiers;
    this.endpoints = endpoints;
    this.machineTags = machineTags;
    this.comments = comments;
    this.datasets = datasets;
    this.installations = installations;
    this.contacts = contacts;
    this.networks = networks;
  }

  public Node newNode() {
    return nodes.newInstance();
  }

  public Organization newOrganization() {
    return organizations.newInstance();
  }

  public Organization newOrganization(UUID endorsingNodeKey) {
    return organizations.newInstance(endorsingNodeKey);
  }

  public Organization newPersistedOrganization() {
    return organizations.newPersistedInstance();
  }

  public Organization newPersistedOrganization(UUID endorsingNodeKey) {
    return organizations.newPersistedInstance(endorsingNodeKey);
  }

  public Identifier newIdentifier() {
    return identifiers.newInstance();
  }

  public Endpoint newEndpoint() {
    Endpoint endpoint = endpoints.newInstance();
    endpoint.addMachineTag(machineTags.newInstance());
    return endpoint;
  }

  public MachineTag newMachineTag() {
    return machineTags.newInstance();
  }

  public Comment newComment() {
    return comments.newInstance();
  }

  public Dataset newDataset(UUID publishingOrganizationKey, UUID installationKey) {
    return datasets.newInstance(publishingOrganizationKey, installationKey);
  }

  public Dataset newPersistedDataset(DOI doi) {
    Organization organization = newPersistedOrganization();
    Installation installation = newPersistedInstallation(organization.getKey());

    return datasets.newPersistedInstance(doi, organization.getKey(), installation.getKey());
  }

  public Dataset newPersistedDeletedDataset(DOI doi) {
    Organization organization = newPersistedOrganization();
    Installation installation = newPersistedInstallation(organization.getKey());

    return datasets.newPersistedDeletedInstance(doi, organization.getKey(), installation.getKey());
  }

  public Dataset newPersistedDataset(UUID organizationKey, UUID installationKey) {
    return datasets.newPersistedInstance(organizationKey, installationKey);
  }

  public Installation newInstallation(UUID organizationKey) {
    return installations.newInstance(organizationKey);
  }

  public Installation newInstallation() {
    return installations.newInstance();
  }

  public Installation newPersistedInstallation() {
    Organization organization = organizations.newPersistedInstance();
    return installations.newPersistedInstance(organization.getKey());
  }

  public Installation newPersistedInstallation(UUID organizationKey) {
    return installations.newPersistedInstance(organizationKey);
  }

  public UsernamePasswordCredentials installationCredentials(Installation installation) {
    return installations.credentials(installation);
  }

  public Contact newContact() {
    return contacts.newInstance();
  }

  public Network newNetwork() {
    return networks.newInstance();
  }

  public Network newPersistedNetwork() {
    return networks.newPersistedInstance();
  }
}
