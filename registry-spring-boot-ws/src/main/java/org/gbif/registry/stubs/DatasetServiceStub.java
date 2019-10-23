package org.gbif.registry.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Qualifier("datasetServiceStub")
public class DatasetServiceStub implements DatasetService {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetServiceStub.class);

  @Override
  public PagingResponse<Dataset> listConstituents(UUID uuid, @Nullable Pageable pageable) {
    LOG.info("DatasetServiceStub#listConstituents");
    return null;
  }

  @Override
  public PagingResponse<Dataset> listConstituents(@Nullable Pageable pageable) {
    LOG.info("DatasetServiceStub#listConstituents");
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByCountry(Country country, @Nullable DatasetType datasetType, @Nullable Pageable pageable) {
    LOG.info("DatasetServiceStub#listByCountry");
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByType(DatasetType datasetType, @Nullable Pageable pageable) {
    LOG.info("DatasetServiceStub#listByType");
    return null;
  }

  @Override
  public List<Metadata> listMetadata(UUID uuid, @Nullable MetadataType metadataType) {
    LOG.info("DatasetServiceStub#listMetadata");
    return Collections.emptyList();
  }

  @Override
  public List<Network> listNetworks(UUID uuid) {
    LOG.info("DatasetServiceStub#listNetworks");
    return Collections.emptyList();
  }

  @Override
  public Metadata getMetadata(int i) {
    LOG.info("DatasetServiceStub#getMetadata");
    return null;
  }

  @Override
  public void deleteMetadata(int i) {
    LOG.info("DatasetServiceStub#deleteMetadata");
  }

  @Override
  public Metadata insertMetadata(UUID uuid, InputStream inputStream) {
    LOG.info("DatasetServiceStub#insertMetadata");
    return null;
  }

  @Override
  public InputStream getMetadataDocument(UUID uuid) {
    LOG.info("DatasetServiceStub#getMetadataDocument");
    return null;
  }

  @Override
  public InputStream getMetadataDocument(int i) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listDeleted(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listDuplicates(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listDatasetsWithNoEndpoint(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByDOI(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public UUID create(@NotNull Dataset dataset) {
    return null;
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    LOG.info("DatasetServiceStub#delete");
  }

  @Override
  public Dataset get(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> collection) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> list(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> search(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByIdentifier(IdentifierType identifierType, String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByIdentifier(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByMachineTag(String s, @Nullable String s1, @Nullable String s2, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public void update(@NotNull Dataset dataset) {
    LOG.info("DatasetServiceStub#update");
  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull Comment comment) {
    return 0;
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {
    LOG.info("DatasetServiceStub#deleteComment");
  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    LOG.info("DatasetServiceStub#listComments");
    return Collections.emptyList();
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    LOG.info("DatasetServiceStub#addContact");
    return 0;
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {
    LOG.info("DatasetServiceStub#deleteContact");
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    LOG.info("DatasetServiceStub#listContacts");
    return Collections.emptyList();
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {
    LOG.info("DatasetServiceStub#updateContact");
  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    LOG.info("DatasetServiceStub#addEndpoint");
    return 0;
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {
    LOG.info("DatasetServiceStub#deleteEndpoint");
  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    LOG.info("DatasetServiceStub#listEndpoints");
    return Collections.emptyList();
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    LOG.info("DatasetServiceStub#addIdentifier");
    return 0;
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {
    LOG.info("DatasetServiceStub#deleteIdentifier");
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    LOG.info("DatasetServiceStub#listIdentifiers");
    return Collections.emptyList();
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    LOG.info("DatasetServiceStub#addMachineTag(UUID, MachineTag)");
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    LOG.info("DatasetServiceStub#addMachineTag(UUID, TagName, String)");
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1, @NotNull String s2) {
    LOG.info("DatasetServiceStub#addMachineTag");
    return 0;
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {
    LOG.info("DatasetServiceStub#deleteMachineTag");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {
    LOG.info("DatasetServiceStub#deleteMachineTags(UUID, TagNamespace)");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {
    LOG.info("DatasetServiceStub#deleteMachineTags(UUID, String)");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {
    LOG.info("DatasetServiceStub#deleteMachineTags(UUID, TagName)");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {
    LOG.info("DatasetServiceStub#deleteMachineTags");
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    LOG.info("DatasetServiceStub#listMachineTags");
    return Collections.emptyList();
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    LOG.info("DatasetServiceStub#addTag");
    return 0;
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    LOG.info("DatasetServiceStub#addTag");
    return 0;
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {
    LOG.info("DatasetServiceStub#deleteTag");
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    LOG.info("DatasetServiceStub#listTags");
    return Collections.emptyList();
  }
}
