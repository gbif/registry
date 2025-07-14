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
package org.gbif.registry.cli.common.stubs;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Dataset.DwcA;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Grid;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.search.DatasetRequestSearchParams;
import org.gbif.api.service.registry.DatasetService;

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.api.vocabulary.TagName;
import org.gbif.api.vocabulary.TagNamespace;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Stub implementation of DatasetService (API service) for CLI modules.
 * Provides minimal functionality needed by DatasetCategoryService.
 */
@Service
public class DatasetServiceStub implements DatasetService {

  @Override
  public UUID create(@NotNull @Valid Dataset dataset) {
    return null;
  }

  @Override
  public void update(Dataset dataset) {
    // No-op for CLI - we don't actually want to update datasets in CLI mode
    // The real implementation would update the database and trigger Elasticsearch updates
  }

  @Override
  public void delete(@NotNull UUID uuid) {

  }

  @Override
  public Dataset get(@NotNull UUID uuid) {
    return null;
  }

  @Override
  public Map<UUID, String> getTitles(@NotNull Collection<UUID> collection) {
    return Map.of();
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
  public PagingResponse<Dataset> listByIdentifier(IdentifierType identifierType, String s,
    @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByIdentifier(String s, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByMachineTag(String s, @Nullable String s1,
    @Nullable String s2, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listConstituents(UUID uuid, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listConstituents(@Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByCountry(Country country, @Nullable DatasetType datasetType,
    @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listByType(DatasetType datasetType, @Nullable Pageable pageable) {
    return null;
  }

  @Override
  public List<Metadata> listMetadata(UUID uuid, @Nullable MetadataType metadataType) {
    return List.of();
  }

  @Override
  public List<Network> listNetworks(UUID uuid) {
    return List.of();
  }

  @Override
  public Metadata getMetadata(int i) {
    return null;
  }

  @Override
  public void deleteMetadata(int i) {

  }

  @Override
  public Metadata insertMetadata(UUID uuid, InputStream inputStream) {
    return null;
  }

  @Override
  public InputStream getMetadataDocument(UUID uuid) {
    return null;
  }

  @Override
  public InputStream getMetadataDocument(int i) {
    return null;
  }

  @Override
  public PagingResponse<Dataset> listDeleted(
    DatasetRequestSearchParams datasetRequestSearchParams) {
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
  public List<Grid> listGrids(UUID uuid) {
    return List.of();
  }

  @Override
  public PagingResponse<Dataset> list(DatasetRequestSearchParams datasetRequestSearchParams) {
    return null;
  }

  @Override
  public void createDwcaData(UUID uuid, DwcA dwcA) {

  }

  @Override
  public void updateDwcaData(UUID uuid, DwcA dwcA) {

  }

  @Override
  public int addComment(@NotNull UUID uuid, @NotNull @Valid Comment comment) {
    return 0;
  }

  @Override
  public void deleteComment(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Comment> listComments(@NotNull UUID uuid) {
    return List.of();
  }

  @Override
  public int addContact(@NotNull UUID uuid, @NotNull @Valid Contact contact) {
    return 0;
  }

  @Override
  public void deleteContact(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Contact> listContacts(@NotNull UUID uuid) {
    return List.of();
  }

  @Override
  public void updateContact(@NotNull UUID uuid, @NotNull @Valid Contact contact) {

  }

  @Override
  public int addEndpoint(@NotNull UUID uuid, @NotNull @Valid Endpoint endpoint) {
    return 0;
  }

  @Override
  public void deleteEndpoint(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Endpoint> listEndpoints(@NotNull UUID uuid) {
    return List.of();
  }

  @Override
  public int addIdentifier(@NotNull UUID uuid, @NotNull @Valid Identifier identifier) {
    return 0;
  }

  @Override
  public void deleteIdentifier(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Identifier> listIdentifiers(@NotNull UUID uuid) {
    return List.of();
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull @Valid MachineTag machineTag) {
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull TagName tagName, @NotNull String s) {
    return 0;
  }

  @Override
  public int addMachineTag(@NotNull UUID uuid, @NotNull String s, @NotNull String s1,
    @NotNull String s2) {
    return 0;
  }

  @Override
  public void deleteMachineTag(@NotNull UUID uuid, int i) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagNamespace tagNamespace) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull TagName tagName) {

  }

  @Override
  public void deleteMachineTags(@NotNull UUID uuid, @NotNull String s, @NotNull String s1) {

  }

  @Override
  public List<MachineTag> listMachineTags(@NotNull UUID uuid) {
    return List.of();
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull String s) {
    return 0;
  }

  @Override
  public int addTag(@NotNull UUID uuid, @NotNull @Valid Tag tag) {
    return 0;
  }

  @Override
  public void deleteTag(@NotNull UUID uuid, int i) {

  }

  @Override
  public List<Tag> listTags(@NotNull UUID uuid, @Nullable String s) {
    return List.of();
  }
}
