package org.gbif.registry.stubs;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.InstallationType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Qualifier("installationServiceStub")
public class InstallationServiceStub implements InstallationService {

  private static final Logger LOG = LoggerFactory.getLogger(InstallationServiceStub.class);

  @Override
  public PagingResponse<Dataset> getHostedDatasets(@NotNull UUID uuid, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Installation> listDeleted(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Installation> listNonPublishing(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public List<KeyTitleResult> suggest(@Nullable String s) {
    return Collections.emptyList();
  }

  @Override
  public PagingResponse<Installation> listByType(InstallationType installationType, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public UUID create(@NotNull Installation installation) {
    return null;
  }

  @Override
  public void delete(@NotNull UUID uuid) {
    LOG.info("InstallationServiceStub#delete");
  }

  @Override
  public Installation get(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public Map<UUID, String> getTitles(Collection<UUID> collection) {
    return null;
  }

  @Override
  public PagingResponse<Installation> list(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Installation> search(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Installation> listByIdentifier(IdentifierType identifierType, String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Installation> listByIdentifier(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Installation> listByMachineTag(String s, @Nullable String s1, @Nullable String s2, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public void update(@NotNull Installation installation) {
    LOG.info("InstallationServiceStub#update");
  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull Comment comment) {
    return 0;
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {
    LOG.info("InstallationServiceStub#deleteComment");
  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    return Collections.emptyList();
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull Contact contact) {
    return 0;
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {
    LOG.info("InstallationServiceStub#deleteContact");
  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    return Collections.emptyList();
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull Contact contact) {
    LOG.info("InstallationServiceStub#updateContact");
  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull Endpoint endpoint) {
    return 0;
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {
    LOG.info("InstallationServiceStub#deleteEndpoint");
  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    return Collections.emptyList();
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull Identifier identifier) {
    return 0;
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {
    LOG.info("InstallationServiceStub#deleteIdentifier");
  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    return Collections.emptyList();
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull MachineTag machineTag) {
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1, @NotNull String s2) {
    return 0;
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {
    LOG.info("InstallationServiceStub#deleteMachineTag");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {
    LOG.info("InstallationServiceStub#deleteMachineTags(UUID, TagNamespace)");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {
    LOG.info("InstallationServiceStub#deleteMachineTags(UUID, String)");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {
    LOG.info("InstallationServiceStub#deleteMachineTags(UUID, TagName)");
  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {
    LOG.info("InstallationServiceStub#deleteMachineTags");
  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    return Collections.emptyList();
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    return 0;
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull Tag tag) {
    return 0;
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {
    LOG.info("InstallationServiceStub#deleteTag");
  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    return Collections.emptyList();
  }
}
