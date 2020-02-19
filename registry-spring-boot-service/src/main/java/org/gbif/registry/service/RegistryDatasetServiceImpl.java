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
package org.gbif.registry.service;

import org.gbif.api.model.Constants;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.metadata.CitationGenerator;
import org.gbif.registry.metadata.parse.DatasetParser;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.handler.ByteArrayWrapper;
import org.gbif.ws.annotation.NullToNotFound;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

@Service
public class RegistryDatasetServiceImpl implements RegistryDatasetService {

  private static final Logger LOG = LoggerFactory.getLogger(RegistryDatasetServiceImpl.class);

  // HTML sanitizer policy for paragraph
  private static final PolicyFactory PARAGRAPH_HTML_SANITIZER =
      new HtmlPolicyBuilder()
          .allowCommonBlockElements() // "p", "div", "h1", ...
          .allowCommonInlineFormattingElements() // "b", "i" ...
          .allowElements("a")
          .allowUrlProtocols("https", "http")
          .allowAttributes("href")
          .onElements("a")
          .toFactory();

  private final DatasetMapper datasetMapper;
  private final MetadataMapper metadataMapper;
  private final LoadingCache<UUID, Organization> organizationCache;

  public RegistryDatasetServiceImpl(
      MetadataMapper metadataMapper,
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper) {
    this.metadataMapper = metadataMapper;
    this.organizationCache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(
                new CacheLoader<UUID, Organization>() {
                  @Override
                  public Organization load(UUID key) {
                    return organizationMapper.get(key);
                  }
                });
    this.datasetMapper = datasetMapper;
  }

  @NullToNotFound
  @Override
  public Dataset get(UUID key) {
    Dataset dataset = merge(getPreferredMetadataDataset(key), datasetMapper.get(key));
    if (dataset == null) {
      return null;
    }

    return sanitizeDataset(dataset);
  }

  /**
   * Sanitize data on Dataset object mainly to restrict HTML tags that can be used.
   *
   * @param dataset
   * @return the original dataset with its content sanitized
   */
  private Dataset sanitizeDataset(Dataset dataset) {
    if (!Strings.isNullOrEmpty(dataset.getDescription())) {
      dataset.setDescription(PARAGRAPH_HTML_SANITIZER.sanitize(dataset.getDescription()));
    }
    return dataset;
  }

  /**
   * Augments a list of datasets with information from their preferred metadata document.
   *
   * @return a the same paging response with a new list of augmented dataset instances
   */
  @Override
  public PagingResponse<Dataset> augmentWithMetadata(PagingResponse<Dataset> resp) {
    List<Dataset> augmented = Lists.newArrayList();
    for (Dataset d : resp.getResults()) {
      augmented.add(merge(getPreferredMetadataDataset(d.getKey()), d));
    }
    resp.setResults(augmented);
    return resp;
  }

  /**
   * Augments the target dataset with all persistable properties from the supplementary dataset.
   * Typically the target would be a dataset built from rich XML metadata, and the supplementary
   * would be the persisted view of the same dataset. NULL values in the supplementary dataset
   * overwrite existing values in the target. Developers please note:
   *
   * <ul>
   *   <li>If the target is null, then the supplementary dataset object itself is returned - not a
   *       copy
   *   <li>These objects are all mutable, and care should be taken that the returned object may be
   *       one or the other of the supplied, thus you need to {@code Dataset result = merge(Dataset
   *       emlView, Dataset dbView);}
   * </ul>
   *
   * @param target that will be modified with persitable values from the supplementary
   * @param supplementary holding the preferred properties for the target
   * @return the modified target dataset, or the supplementary dataset if the target is null
   */
  private Dataset merge(@Nullable Dataset target, @Nullable Dataset supplementary) {

    // nothing to merge, return the target (which may be null)
    if (supplementary == null) {
      setGeneratedCitation(target);
      return target;
    }

    // nothing to overlay into
    if (target == null) {
      setGeneratedCitation(supplementary);
      return supplementary;
    }

    // otherwise, copy all persisted values
    target.setKey(supplementary.getKey());
    target.setDoi(supplementary.getDoi());
    target.setParentDatasetKey(supplementary.getParentDatasetKey());
    target.setDuplicateOfDatasetKey(supplementary.getDuplicateOfDatasetKey());
    target.setInstallationKey(supplementary.getInstallationKey());
    target.setPublishingOrganizationKey(supplementary.getPublishingOrganizationKey());
    target.setExternal(supplementary.isExternal());
    target.setNumConstituents(supplementary.getNumConstituents());
    target.setType(supplementary.getType());
    target.setSubtype(supplementary.getSubtype());
    target.setTitle(supplementary.getTitle());
    target.setAlias(supplementary.getAlias());
    target.setAbbreviation(supplementary.getAbbreviation());
    target.setDescription(supplementary.getDescription());
    target.setLanguage(supplementary.getLanguage());
    target.setHomepage(supplementary.getHomepage());
    target.setLogoUrl(supplementary.getLogoUrl());
    target.setCitation(supplementary.getCitation());
    target.setRights(supplementary.getRights());
    target.setLicense(supplementary.getLicense());
    target.setMaintenanceUpdateFrequency(supplementary.getMaintenanceUpdateFrequency());
    target.setLockedForAutoUpdate(supplementary.isLockedForAutoUpdate());
    target.setCreated(supplementary.getCreated());
    target.setCreatedBy(supplementary.getCreatedBy());
    target.setModified(supplementary.getModified());
    target.setModifiedBy(supplementary.getModifiedBy());
    target.setDeleted(supplementary.getDeleted());
    // nested properties
    target.setComments(supplementary.getComments());
    target.setContacts(supplementary.getContacts());
    target.setEndpoints(supplementary.getEndpoints());
    target.setIdentifiers(supplementary.getIdentifiers());
    target.setMachineTags(supplementary.getMachineTags());
    target.setTags(supplementary.getTags());

    setGeneratedCitation(target);

    return target;
  }

  /**
   * Set the generated GBIF citation on the provided Dataset object. This function is used until we
   * decide if we store the GBIF generated citation in the database.
   *
   * <p>see https://github.com/gbif/registry/issues/4
   *
   * @param dataset
   * @return
   */
  private void setGeneratedCitation(Dataset dataset) {
    if (dataset != null
        && dataset.getPublishingOrganizationKey() != null
        // for CoL and its constituents we want to show the verbatim citation and no GBIF generated
        // one:
        // https://github.com/gbif/portal-feedback/issues/1819
        && !Constants.COL_DATASET_KEY.equals(dataset.getKey())
        && !Constants.COL_DATASET_KEY.equals(dataset.getParentDatasetKey())) {

      // if the citation already exists keep it and only change the text. That allows us to keep the
      // identifier
      // if provided.
      Citation citation = dataset.getCitation() == null ? new Citation() : dataset.getCitation();
      citation.setText(
          CitationGenerator.generateCitation(
              dataset, organizationCache.getUnchecked(dataset.getPublishingOrganizationKey())));
      dataset.setCitation(citation);
    }
  }

  /** Returns the parsed, preferred metadata document as a dataset. */
  @Nullable
  private Dataset getPreferredMetadataDataset(UUID key) {
    List<Metadata> docs = listMetadata(key, null);
    if (!docs.isEmpty()) {
      InputStream stream = null;
      try {
        // the list is sorted by priority already, just pick the first!
        stream = getMetadataDocument(docs.get(0).getKey());
        return DatasetParser.build(stream);
      } catch (IOException | IllegalArgumentException e) {
        // Not sure if we should not propagate an Exception to return a 500 instead
        LOG.error("Stored metadata document {} cannot be read", docs.get(0).getKey(), e);
      } finally {
        Closeables.closeQuietly(stream);
      }
    }

    return null;
  }

  @Override
  public List<Metadata> listMetadata(UUID datasetKey, @Nullable MetadataType type) {
    return metadataMapper.list(datasetKey, type);
  }

  @NullToNotFound
  @Override
  public InputStream getMetadataDocument(int metadataKey) {
    ByteArrayWrapper document = metadataMapper.getDocument(metadataKey);
    if (document == null) {
      return null;
    }
    return new ByteArrayInputStream(document.getData());
  }

  @Override
  public List<UUID> owningEntityKeys(@NotNull Dataset entity) {
    List<UUID> keys = new ArrayList<>();
    keys.add(entity.getPublishingOrganizationKey());
    keys.add(
        organizationCache
            .getUnchecked(entity.getPublishingOrganizationKey())
            .getEndorsingNodeKey());
    return keys;
  }
}
