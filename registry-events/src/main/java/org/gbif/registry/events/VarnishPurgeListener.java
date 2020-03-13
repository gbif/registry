/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.events;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.service.collections.PersonService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.events.collections.CreateCollectionEntityEvent;
import org.gbif.registry.events.collections.DeleteCollectionEntityEvent;
import org.gbif.registry.events.collections.UpdateCollectionEntityEvent;
import org.gbif.varnish.VarnishPurger;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.eventbus.Subscribe;

/**
 * A event bus listener that will flush varnish if registry entities like datasets and organizations
 * have been created, modified or deleted.
 *
 * <p>Varnish provides two main ways of invalidating its cache: PURGE and BAN. PURGE truly frees the
 * cache, but only works on individual resource URLs while BANs work with regular expressions and
 * can banRegex entire subresources from being served. BANs do not remove the object from the
 * varnish memory though.
 *
 * @see <h ref="https://www.varnish-software.com/static/book/Cache_invalidation.html">Varnish
 *     Book</h>
 *     <h3>Purging cascade logic</h3>
 *     A quick overview of the logic how entity changes trigger purges on other resources. In case
 *     of updates all keys used from the changed objects need be taken from both the old and new
 *     version.
 *     <h4>Node</h4>
 *     <p>Nodes are only modified in the IMS and no events are ever triggered.
 *     <ul>
 *       <li>Node detail PURGE
 *       <li>List all nodes BAN
 *     </ul>
 *     <h4>Organization</h4>
 *     <ul>
 *       <li>Organization detail PURGE
 *       <li>List all organizations BAN
 *       <li>Publisher search & suggest BAN
 *       <li>/node/{org.endorsingNodeKey}/organization
 *     </ul>
 *     <h4>Network</h4>
 *     <ul>
 *       <li>Network detail PURGE
 *       <li>List all networks BAN
 *     </ul>
 *     <h4>Installation</h4>
 *     <ul>
 *       <li>Installation detail PURGE
 *       <li>List all installations BAN
 *       <li>/node/{i.organization.endorsingNodeKey}/installation BAN
 *       <li>/organization/{UUID}/installation BAN
 *     </ul>
 *     <h4>Dataset</h4>
 *     <ul>
 *       <li>dataset/{key} PURGE
 *       <li>dataset/{d.parentKey} PURGE
 *       <li>dataset/{d.parentKey}/constituents BAN
 *       <li>dataset BAN
 *       <li>dataset/search|suggest BAN
 *       <li>/installation/{d.installationKey}/dataset BAN
 *       <li>/organization/{d.installation.organizationKey}/hostedDataset BAN
 *       <li>/organization/{d.publishingOrganizationKey}/publishedDataset BAN
 *       <li>/node/{d.publishingOrganization.endorsingNodeKey}/dataset BAN
 *       <li>/network/{any UUID}/constituents BAN
 *     </ul>
 */
public class VarnishPurgeListener {

  private static final Logger LOG = LoggerFactory.getLogger(VarnishPurgeListener.class);
  private final OrganizationService organizationService;
  private final InstallationService installationService;
  private final DatasetService datasetService;
  private final InstitutionService institutionService;
  private final CollectionService collectionService;
  private final PersonService personService;
  private final VarnishPurger purger;
  private static final Joiner PATH_JOINER = Joiner.on("/").skipNulls();

  public VarnishPurgeListener(
      CloseableHttpClient client,
      EventManager eventManager,
      URI apiBaseUrl,
      OrganizationService organizationService,
      InstallationService installationService,
      DatasetService datasetService,
      InstitutionService institutionService,
      CollectionService collectionService,
      PersonService personService) {
    this.organizationService = organizationService;
    this.installationService = installationService;
    this.datasetService = datasetService;
    this.institutionService = institutionService;
    this.collectionService = collectionService;
    this.personService = personService;
    eventManager.register(this);

    purger = new VarnishPurger(client, apiBaseUrl);
  }

  @Subscribe
  public final <T extends NetworkEntity> void created(CreateEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getNewObject().getKey());

    if (event.getObjectClass().equals(Organization.class)) {
      cascadeOrganizationChange((Organization) event.getNewObject());
    } else if (event.getObjectClass().equals(Dataset.class)) {
      cascadeDatasetChange((Dataset) event.getNewObject());
    } else if (event.getObjectClass().equals(Installation.class)) {
      cascadeInstallationChange((Installation) event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void updated(UpdateEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getOldObject().getKey());

    if (event.getObjectClass().equals(Organization.class)) {
      cascadeOrganizationChange(
          (Organization) event.getOldObject(), (Organization) event.getNewObject());
    } else if (event.getObjectClass().equals(Dataset.class)) {
      cascadeDatasetChange((Dataset) event.getOldObject(), (Dataset) event.getNewObject());
    } else if (event.getObjectClass().equals(Installation.class)) {
      cascadeInstallationChange(
          (Installation) event.getOldObject(), (Installation) event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends NetworkEntity> void deleted(DeleteEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getOldObject().getKey());

    if (event.getObjectClass().equals(Organization.class)) {
      cascadeOrganizationChange((Organization) event.getOldObject());
    } else if (event.getObjectClass().equals(Dataset.class)) {
      cascadeDatasetChange((Dataset) event.getOldObject());
    } else if (event.getObjectClass().equals(Installation.class)) {
      cascadeInstallationChange((Installation) event.getOldObject());
    }
  }

  @Subscribe
  public final <T extends CollectionEntity> void createdCollection(
      CreateCollectionEntityEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getNewObject().getKey());

    if (event.getObjectClass().equals(Person.class)) {
      cascadePersonChange((Person) event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends CollectionEntity> void updatedCollection(
      UpdateCollectionEntityEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getOldObject().getKey());

    if (event.getObjectClass().equals(Person.class)) {
      cascadePersonChange((Person) event.getOldObject(), (Person) event.getNewObject());
    }
  }

  @Subscribe
  public final <T extends CollectionEntity> void deletedCollection(
      DeleteCollectionEntityEvent<T> event) {
    purgeEntityAndBanLists(event.getObjectClass(), event.getOldObject().getKey());

    if (event.getObjectClass().equals(Person.class)) {
      cascadePersonChange((Person) event.getOldObject());
    }
  }

  @Subscribe
  public final void componentChange(ChangedComponentEvent event) {
    purgeEntityAndBanLists(event.getTargetClass(), event.getTargetEntityKey());
    // keys have not changed, only some component of the entity itself
    if (event.getTargetClass().equals(Organization.class)) {
      cascadeOrganizationChange(organizationService.get(event.getTargetEntityKey()));
    } else if (event.getTargetClass().equals(Dataset.class)) {
      cascadeDatasetChange(datasetService.get(event.getTargetEntityKey()));
    } else if (event.getTargetClass().equals(Installation.class)) {
      cascadeInstallationChange(installationService.get(event.getTargetEntityKey()));
    } else if (event.getTargetClass().equals(Person.class)) {
      cascadePersonChange((Person) personService.get(event.getTargetEntityKey()));
    }
  }

  // group bans by entity class to avoid too many ban rules and thus bad varnish performance
  private void cascadeDatasetChange(Dataset... datasets) {
    // UUIDHashSet ignores null values
    Set<UUID> instKeys = new UUIDHashSet();
    Set<UUID> orgKeys = new UUIDHashSet();
    Set<UUID> nodeKeys = new UUIDHashSet();
    Set<UUID> parentKeys = new UUIDHashSet();
    for (Dataset d : datasets) {
      if (!orgKeys.contains(d.getPublishingOrganizationKey())) {
        Organization o = organizationService.get(d.getPublishingOrganizationKey());
        nodeKeys.add(o.getEndorsingNodeKey());
      }
      if (!instKeys.contains(d.getInstallationKey())) {
        instKeys.add(d.getInstallationKey());
        Installation i = installationService.get(d.getInstallationKey());
        orgKeys.add(i.getOrganizationKey());
      }
      orgKeys.add(d.getPublishingOrganizationKey());
      if (d.getParentDatasetKey() != null) {
        parentKeys.add(d.getParentDatasetKey());
        purger.purge(path("dataset", d.getParentDatasetKey()));
      }
    }
    purger.ban(String.format("dataset/%s/constituents", purger.anyKey(parentKeys)));
    // /installation/{d.installationKey}/dataset BAN
    purger.ban(String.format("installation/%s/dataset", purger.anyKey(instKeys)));
    // /organization/{d.publishingOrganizationKey}/publishedDataset BAN
    // /organization/{d.installation.organizationKey}/hostedDataset BAN
    purger.ban(String.format("organization/%s/(published|hosted)Dataset", purger.anyKey(orgKeys)));
    // /node/{d.publishingOrganization.endorsingNodeKey}/dataset BAN
    purger.ban(String.format("node/%s/dataset", purger.anyKey(nodeKeys)));
    // /network/{any UUID}/constituents BAN
    purger.ban(String.format("network/.+/constituents"));
  }

  private void cascadeOrganizationChange(Organization... orgs) {
    // /node/{org.endorsingNodeKey}/
    Set<UUID> nodeKeys = new UUIDHashSet();
    for (Organization o : orgs) {
      nodeKeys.add(o.getEndorsingNodeKey());
    }
    purger.ban(String.format("node/%s/organization", purger.anyKey(nodeKeys)));
  }

  private void cascadeInstallationChange(Installation... installations) {
    Set<UUID> keys = new UUIDHashSet();
    // /organization/{i.organizationKey}/installation BAN
    for (Installation i : installations) {
      keys.add(i.getOrganizationKey());
    }
    purger.ban(String.format("organization/%s/installation", purger.anyKey(keys)));

    // /node/{i.organization.endorsingNodeKey}/installation BAN
    Set<UUID> nodekeys = new UUIDHashSet();
    for (UUID orgKey : keys) {
      Organization o = organizationService.get(orgKey);
      nodekeys.add(o.getEndorsingNodeKey());
    }
    purger.ban(String.format("%node/%s/organization", purger.anyKey(nodekeys)));
  }

  private void cascadePersonChange(Person... persons) {
    Set<UUID> collectionKeys = new UUIDHashSet();
    for (Person p : persons) {
      List<Collection> collections =
          collectionService.list(null, null, p.getKey(), null, null, null).getResults();
      collections.forEach(c -> collectionKeys.add(c.getKey()));
    }

    Set<UUID> institutionKeys = new UUIDHashSet();
    for (Person p : persons) {
      List<Institution> institutions =
          institutionService.list(null, p.getKey(), null, null, null).getResults();
      institutions.forEach(i -> institutionKeys.add(i.getKey()));
    }

    // /collection/{collectionKey}/contact BAN
    purger.ban(String.format("collection/%s/contact", purger.anyKey(collectionKeys)));
    // /institution/{institutionKey}/contact BAN
    purger.ban(String.format("institution/%s/contact", purger.anyKey(institutionKeys)));
  }

  /**
   * Removes the specific entity from varnish and bans search & list pages. This method does not
   * check which entity class was supplied, but as it is some type of NetworkEntity we deal with the
   * right urls.
   */
  private void purgeEntityAndBanLists(Class cl, UUID key) {

    // purge entity detail
    purger.purge(path(cl.getSimpleName().toLowerCase(), key));

    // banRegex lists and searches
    purger.ban(String.format("%s(/search|/suggest)?[^/]*$", cl.getSimpleName().toLowerCase()));
  }

  private static String path(Object... parts) {
    return PATH_JOINER.join(parts);
  }

  /** HashSet with an overriden add method that silently ignores null values being added. */
  public class UUIDHashSet extends HashSet<UUID> {
    @Override
    public boolean add(UUID t) {
      return t != null && super.add(t);
    }
  }
}
