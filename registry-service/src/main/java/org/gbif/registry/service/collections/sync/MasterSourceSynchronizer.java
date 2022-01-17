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
package org.gbif.registry.service.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.events.collections.MasterSourceMetadataAddedEvent;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.dto.MasterSourceOrganizationDto;
import org.gbif.registry.service.collections.converters.CollectionConverter;
import org.gbif.registry.service.collections.converters.InstitutionConverter;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Component
public class MasterSourceSynchronizer {

  private final CollectionService collectionService;
  private final InstitutionService institutionService;
  private final OrganizationMapper organizationMapper;
  private final DatasetMapper datasetMapper;
  private final CollectionMapper collectionMapper;
  private final CollectionsEmailManager emailManager;
  private final EmailSender emailSender;

  @Autowired
  public MasterSourceSynchronizer(
      CollectionService collectionService,
      InstitutionService institutionService,
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      CollectionMapper collectionMapper,
      CollectionsEmailManager emailManager,
      EmailSender emailSender,
      EventManager eventManager) {
    this.collectionService = collectionService;
    this.institutionService = institutionService;
    this.organizationMapper = organizationMapper;
    this.datasetMapper = datasetMapper;
    this.collectionMapper = collectionMapper;
    this.emailManager = emailManager;
    this.emailSender = emailSender;
    eventManager.register(this);
  }

  @Subscribe
  public <T> void syncMasterSourceChange(UpdateEvent<T> event) {
    if (Dataset.class.isAssignableFrom(event.getObjectClass())) {
      Dataset updatedDataset = (Dataset) event.getNewObject();

      // find if there is any collection whose master source is this dataset
      List<Collection> collectionFound =
          collectionService.findByMasterSource(Source.DATASET, updatedDataset.getKey().toString());

      if (collectionFound.size() > 1) {
        log.warn(
            "Multiple collections found with master source dataset {}", updatedDataset.getKey());
      }

      collectionFound.forEach(
          collection -> {
            log.info(
                "Updating collection {} with changes from dataset {}",
                collection.getKey(),
                updatedDataset.getKey());

            // update the collection
            Organization publishingOrganization =
                organizationMapper.get(updatedDataset.getPublishingOrganizationKey());

            updateCollection(updatedDataset, publishingOrganization, collectionFound.get(0));
          });

    } else if (Organization.class.isAssignableFrom(event.getObjectClass())) {
      Organization updatedOrganization = (Organization) event.getNewObject();

      // find if there is any institution whose master source is this organization
      List<Institution> institutionFound =
          institutionService.findByMasterSource(
              Source.ORGANIZATION, updatedOrganization.getKey().toString());

      if (institutionFound.size() > 1) {
        log.warn(
            "Multiple institutions found with master source organization {}",
            updatedOrganization.getKey());
      }

      // update the institutions
      institutionFound.forEach(
          institution -> {
            log.info(
                "Updating institution {} with changes from organization {}",
                institution.getKey(),
                updatedOrganization.getKey());
            updateInstitution(updatedOrganization, institutionFound.get(0));
          });

      // find if there are collections whose master source is a dataset with this organization as
      // publisher since the collections get some info from the publishing organization
      List<MasterSourceOrganizationDto> collectionsFound =
          collectionMapper.findByDatasetOrganizationAsMasterSource(updatedOrganization.getKey());

      // update the collections found
      collectionsFound.forEach(
          dto -> {
            log.info(
                "Updating collection {} with changes from organization {} and dataset {} as source",
                dto.getCollectionKey(),
                updatedOrganization.getKey(),
                dto.getDatasetKey());

            Dataset dataset = datasetMapper.get(dto.getDatasetKey());
            Collection collection = collectionService.get(dto.getCollectionKey());

            updateCollection(dataset, updatedOrganization, collection);
          });
    }
  }

  @Subscribe
  public <T> void syncDeletedMasterSource(DeleteEvent<T> event) {
    if (Dataset.class.isAssignableFrom(event.getObjectClass())) {
      Dataset dataset = (Dataset) event.getOldObject();

      // find if there is any collection whose master source is this dataset
      List<Collection> collectionFound =
          collectionService.findByMasterSource(Source.DATASET, dataset.getKey().toString());

      collectionFound.forEach(
          collection -> {
            log.info(
                "Master source dataset {} of collection {} deleted",
                dataset.getKey(),
                collection.getKey());

            // remove the metadata
            collectionService.deleteMasterSourceMetadata(collection.getKey());

            // create comment
            Comment comment = new Comment();
            comment.setContent(
                "The master source of this collection used to be a dataset that was deleted: "
                    + dataset.getKey().toString());
            collectionService.addComment(collection.getKey(), comment);

            sendEmail(
                collection.getKey(),
                collection.getName(),
                CollectionEntityType.COLLECTION,
                dataset.getKey(),
                dataset.getTitle(),
                "dataset");
          });
    } else if (Organization.class.isAssignableFrom(event.getObjectClass())) {
      Organization organization = (Organization) event.getOldObject();

      // find if there is any institution whose master source is this organization
      List<Institution> institutionFound =
          institutionService.findByMasterSource(
              Source.ORGANIZATION, organization.getKey().toString());

      institutionFound.forEach(
          institution -> {
            log.info(
                "Master source organization {} of institution {} deleted",
                organization.getKey(),
                institution.getKey());

            // remove the metadata
            institutionService.deleteMasterSourceMetadata(institution.getKey());

            // create comment
            Comment comment = new Comment();
            comment.setContent(
                "The master source of this institution used to be an organization that was deleted: "
                    + organization.getKey().toString());
            institutionService.addComment(institution.getKey(), comment);

            // send email
            sendEmail(
                institution.getKey(),
                institution.getName(),
                CollectionEntityType.INSTITUTION,
                organization.getKey(),
                organization.getTitle(),
                "organization");
          });
    }
  }

  private void sendEmail(
      UUID entityKey,
      String name,
      CollectionEntityType collectionEntityType,
      UUID masterSourceEntityKey,
      String masterSourceName,
      String masterSourceType) {
    // send email
    try {
      BaseEmailModel emailModel =
          emailManager.generateMasterSourceDeletedEmailModel(
              entityKey,
              name,
              collectionEntityType,
              masterSourceEntityKey,
              masterSourceName,
              masterSourceType);
      emailSender.send(emailModel);
    } catch (Exception e) {
      log.error("Couldn't send email for GRSciColl master source deleted", e);
    }
  }

  @Subscribe
  public void syncNewMasterSource(MasterSourceMetadataAddedEvent event) {
    if (event.getMetadata().getSource() == Source.DATASET) {
      Collection collection = collectionService.get(event.getCollectionEntityKey());
      checkArgument(
          collection != null, "Collection not found for key " + event.getCollectionEntityKey());

      Dataset dataset = datasetMapper.get(UUID.fromString(event.getMetadata().getSourceId()));
      Organization publishingOrganization =
          organizationMapper.get(dataset.getPublishingOrganizationKey());

      log.info(
          "Updating collection {} with new master source dataset {}",
          collection.getKey(),
          dataset.getKey());

      updateCollection(dataset, publishingOrganization, collection);
    } else if (event.getMetadata().getSource() == Source.ORGANIZATION) {
      Institution institution = institutionService.get(event.getCollectionEntityKey());
      checkArgument(
          institution != null, "Institution not found for key " + event.getCollectionEntityKey());

      Organization organization =
          organizationMapper.get(UUID.fromString(event.getMetadata().getSourceId()));

      log.info(
          "Updating institution {} with new master source organization {}",
          institution.getKey(),
          organization.getKey());

      updateInstitution(organization, institution);
    }
  }

  private void updateCollection(
      Dataset dataset, Organization publishingOrganization, Collection existingCollection) {
    Collection convertedCollection =
        CollectionConverter.convertFromDataset(dataset, publishingOrganization, existingCollection);

    // create new identifiers
    if (convertedCollection.getIdentifiers().stream().anyMatch(i -> i.getKey() == null)) {
      convertedCollection.getIdentifiers().stream()
          .filter(i -> i.getKey() == null)
          .forEach(i -> collectionService.addIdentifier(existingCollection.getKey(), i));
      // update the identifiers to pass the constraints validations when updating the entity
      convertedCollection.setIdentifiers(
          collectionService.listIdentifiers(existingCollection.getKey()));
    }

    // replace contacts
    collectionService.replaceContactPersons(
        existingCollection.getKey(), convertedCollection.getContactPersons());

    // update collection
    collectionService.update(convertedCollection, false);
  }

  private void updateInstitution(Organization organization, Institution existingInstitution) {
    Institution convertedInstitution =
        InstitutionConverter.convertFromOrganization(organization, existingInstitution);

    // replace contacts
    institutionService.replaceContactPersons(
        existingInstitution.getKey(), convertedInstitution.getContactPersons());

    // update institution
    institutionService.update(convertedInstitution, false);
  }
}
