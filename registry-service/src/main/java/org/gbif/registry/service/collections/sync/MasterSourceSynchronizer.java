package org.gbif.registry.service.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.CollectionEntityType;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceType;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.DeleteEvent;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.events.UpdateEvent;
import org.gbif.registry.events.collections.EventType;
import org.gbif.registry.events.collections.SubEntityCollectionEvent;
import org.gbif.registry.mail.BaseEmailModel;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.collections.CollectionsEmailManager;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.service.collections.converters.CollectionConverter;
import org.gbif.registry.service.collections.converters.InstitutionConverter;
import org.gbif.registry.service.collections.utils.MasterSourceUtils;

import java.util.Optional;
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
  private final CollectionsEmailManager emailManager;
  private final EmailSender emailSender;

  @Autowired
  public MasterSourceSynchronizer(
      CollectionService collectionService,
      InstitutionService institutionService,
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      CollectionsEmailManager emailManager,
      EmailSender emailSender,
      EventManager eventManager) {
    this.collectionService = collectionService;
    this.institutionService = institutionService;
    this.organizationMapper = organizationMapper;
    this.datasetMapper = datasetMapper;
    this.emailManager = emailManager;
    this.emailSender = emailSender;
    eventManager.register(this);
  }

  @Subscribe
  public <T> void syncMasterSourceChange(UpdateEvent<T> event) {
    if (Dataset.class.isAssignableFrom(event.getObjectClass())) {
      Dataset updatedDataset = (Dataset) event.getNewObject();

      // find if there is any collection whose master source is this dataset
      Optional<Collection> collectionFound = findCollectionsSourcedFromDataset(updatedDataset);

      if (collectionFound.isPresent()) {
        // update the collection
        Organization publishingOrganization =
            organizationMapper.get(updatedDataset.getPublishingOrganizationKey());

        Collection updatedCollection =
            CollectionConverter.convertFromDataset(
                updatedDataset, publishingOrganization, collectionFound.get());
        collectionService.update(updatedCollection);
      }
    } else if (Organization.class.isAssignableFrom(event.getObjectClass())) {
      Organization updatedOrganization = (Organization) event.getNewObject();

      // find if there is any institution whose master source is this organization
      Optional<Institution> institutionFound =
          findInstitutionsSourcedFromOrganization(updatedOrganization);

      if (institutionFound.isPresent()) {
        // update the institution
        Institution updatedInstitution =
            InstitutionConverter.convertFromOrganization(
                updatedOrganization, institutionFound.get());
        institutionService.update(updatedInstitution);
      }
    }
  }

  @Subscribe
  public <T> void syncDeletedMasterSource(DeleteEvent<T> event) {
    if (Dataset.class.isAssignableFrom(event.getObjectClass())) {
      Dataset dataset = (Dataset) event.getOldObject();

      // find if there is any collection whose master source is this dataset
      Optional<Collection> collectionFound = findCollectionsSourcedFromDataset(dataset);

      if (collectionFound.isPresent()) {
        Collection collection = collectionFound.get();

        // remove the machine tag
        collection.getMachineTags().stream()
            .filter(
                mt ->
                    mt.getNamespace().equals(MasterSourceUtils.MASTER_SOURCE_COLLECTIONS_NAMESPACE)
                        && mt.getName().equals(MasterSourceUtils.DATASET_SOURCE)
                        && mt.getValue().equals(dataset.getKey().toString()))
            .forEach(mt -> collectionService.deleteMachineTag(collection.getKey(), mt.getKey()));

        // set the master source to GRSciColl
        collectionService.updateMasterSource(collection.getKey(), MasterSourceType.GRSCICOLL);

        // create comment
        Comment comment = new Comment();
        comment.setContent(
            "The master source of this collection used to be a dataset that was deleted: "
                + dataset.getKey().toString());
        collectionService.addComment(collection.getKey(), comment);

        // send email
        try {
          BaseEmailModel emailModel =
              emailManager.generateMasterSourceDeletedEmailModel(
                  collection.getKey(),
                  collection.getName(),
                  CollectionEntityType.COLLECTION,
                  dataset.getKey(),
                  dataset.getTitle(),
                  "dataset");
          emailSender.send(emailModel);
        } catch (Exception e) {
          log.error("Couldn't send email for GRSciColl master source dataset deleted", e);
        }
      }
    } else if (Organization.class.isAssignableFrom(event.getObjectClass())) {
      Organization organization = (Organization) event.getOldObject();

      // find if there is any institution whose master source is this organization
      Optional<Institution> institutionFound =
          findInstitutionsSourcedFromOrganization(organization);

      if (institutionFound.isPresent()) {
        Institution institution = institutionFound.get();

        // remove the machine tag
        institution.getMachineTags().stream()
            .filter(
                mt ->
                    mt.getNamespace().equals(MasterSourceUtils.MASTER_SOURCE_COLLECTIONS_NAMESPACE)
                        && mt.getName().equals(MasterSourceUtils.ORGANIZATION_SOURCE)
                        && mt.getValue().equals(organization.getKey().toString()))
            .forEach(mt -> institutionService.deleteMachineTag(institution.getKey(), mt.getKey()));

        // set the master source to GRSciColl
        institutionService.updateMasterSource(institution.getKey(), MasterSourceType.GRSCICOLL);

        // create comment
        Comment comment = new Comment();
        comment.setContent(
            "The master source of this institution used to be an organization that was deleted: "
                + organization.getKey().toString());
        institutionService.addComment(institution.getKey(), comment);

        // send email
        try {
          BaseEmailModel emailModel =
              emailManager.generateMasterSourceDeletedEmailModel(
                  institution.getKey(),
                  institution.getName(),
                  CollectionEntityType.INSTITUTION,
                  organization.getKey(),
                  organization.getTitle(),
                  "organization");
          emailSender.send(emailModel);
        } catch (Exception e) {
          log.error("Couldn't send email for GRSciColl master source organization deleted", e);
        }
      }
    }
  }

  @Subscribe
  public <T extends CollectionEntity, R> void syncNewMasterSource(
      SubEntityCollectionEvent<T, R> event) {

    // TODO: este va a saltar cuando haga un createFromDataset y add el tag

    // we only care about the creation of machine tags for master sources
    if (event.getEventType() == EventType.CREATE
        && MachineTag.class.isAssignableFrom(event.getSubEntityClass())) {
      MachineTag machineTag = (MachineTag) event.getSubEntity();

      if (!machineTag
          .getNamespace()
          .equals(MasterSourceUtils.MASTER_SOURCE_COLLECTIONS_NAMESPACE)) {
        return;
      }

      if (machineTag.getName().equals(MasterSourceUtils.DATASET_SOURCE)) {
        Collection collection = collectionService.get(event.getCollectionEntityKey());
        checkArgument(
            collection != null, "Collection not found for key " + event.getCollectionEntityKey());

        Dataset dataset = datasetMapper.get(UUID.fromString(machineTag.getValue()));
        Organization publishingOrganization =
            organizationMapper.get(dataset.getPublishingOrganizationKey());
        collectionService.update(
            CollectionConverter.convertFromDataset(dataset, publishingOrganization, collection));
      } else if (machineTag.getName().equals(MasterSourceUtils.ORGANIZATION_SOURCE)) {
        Institution institution = institutionService.get(event.getCollectionEntityKey());
        checkArgument(
            institution != null, "Institution not found for key " + event.getCollectionEntityKey());

        Organization organization = organizationMapper.get(UUID.fromString(machineTag.getValue()));

        institutionService.update(
            InstitutionConverter.convertFromOrganization(organization, institution));
      }
    }
  }

  private Optional<Institution> findInstitutionsSourcedFromOrganization(
      Organization updatedOrganization) {
    PagingResponse<Institution> institutionsFound =
        institutionService.listByMachineTag(
            MasterSourceUtils.MASTER_SOURCE_COLLECTIONS_NAMESPACE,
            MasterSourceUtils.ORGANIZATION_SOURCE,
            updatedOrganization.getKey().toString(),
            new PagingRequest(0, 2));

    if (institutionsFound.getCount() > 1) {
      throw new IllegalArgumentException(
          "Found more than 1 institution with master source organization "
              + updatedOrganization.getKey());
    }

    if (institutionsFound.getCount() == 1) {
      return Optional.of(institutionsFound.getResults().get(0));
    }

    return Optional.empty();
  }

  private Optional<Collection> findCollectionsSourcedFromDataset(Dataset updatedDataset) {
    PagingResponse<Collection> collectionsFound =
        collectionService.listByMachineTag(
            MasterSourceUtils.MASTER_SOURCE_COLLECTIONS_NAMESPACE,
            MasterSourceUtils.DATASET_SOURCE,
            updatedDataset.getKey().toString(),
            new PagingRequest(0, 2));

    if (collectionsFound.getCount() > 1) {
      throw new IllegalArgumentException(
          "Found more than 1 collection with master source dataset " + updatedDataset.getKey());
    }

    if (collectionsFound.getCount() == 1) {
      return Optional.of(collectionsFound.getResults().get(0));
    }

    return Optional.empty();
  }
}
