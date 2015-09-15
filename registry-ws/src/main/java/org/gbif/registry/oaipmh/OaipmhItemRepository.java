package org.gbif.registry.oaipmh;

import com.google.common.io.Closeables;
import com.google.inject.Inject;
import com.lyncode.builder.ListBuilder;
import com.lyncode.xoai.dataprovider.exceptions.IdDoesNotExistException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.filter.ScopedFilter;
import com.lyncode.xoai.dataprovider.handlers.helpers.ItemRepositoryHelper;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemIdentifiersResult;
import com.lyncode.xoai.dataprovider.handlers.results.ListItemsResults;
import com.lyncode.xoai.dataprovider.model.InMemoryItem;
import com.lyncode.xoai.dataprovider.model.Item;
import com.lyncode.xoai.dataprovider.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.gbif.api.exception.ServiceUnavailableException;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.metadata.parse.DatasetParser;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.MetadataMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

/**
 * Created by mblissett on 14/09/15.
 */
@Singleton
public class OaipmhItemRepository implements ItemRepository {

  private static final Logger LOG = LoggerFactory.getLogger(OaipmhItemRepository.class);

  private final DatasetMapper datasetMapper;
  private final MetadataMapper metadataMapper;

  @Inject
  public OaipmhItemRepository(DatasetMapper datasetMapper, MetadataMapper metadataMapper) {
    this.datasetMapper = datasetMapper;
    this.metadataMapper = metadataMapper;
  }

  @Override
  public Item getItem(String s) throws IdDoesNotExistException, OAIException {

    // the fully augmented dataset
    Dataset dataset = get(UUID.fromString(s));
    if (dataset != null) {
      String country = "Denmark";

      InMemoryItem imi = InMemoryItem.item()
              .withIdentifier(s)
              .with("datestamp", dataset.getModified())
              .with("sets", new ListBuilder<String>().add(country).build())
              .with("deleted", true | dataset.getDeleted() != null); // TODO: set deleted so metadata isn't printed.

      return imi;
    }

    return null;
  }

  public Dataset get(UUID key) {
    return merge(getPreferredMetadataDataset(key), WithMyBatis.get(datasetMapper, key));
  }

  /**
   * Augments the target dataset with all persistable properties from the supplementary dataset.
   * Typically the target would be a dataset built from rich XML metadata, and the supplementary would be the persisted
   * view of the same dataset. NULL values in the supplementary dataset overwrite existing values in the target.
   * Developers please note:
   * <ul>
   * <li>If the target is null, then the supplementary dataset object itself is returned - not a copy</li>
   * <li>These objects are all mutable, and care should be taken that the returned object may be one or the other of
   * the
   * supplied, thus you need to {@code Dataset result = merge(Dataset emlView, Dataset dbView);}</li>
   * </ul>
   *
   * @param target        that will be modified with persitable values from the supplementary
   * @param supplementary holding the preferred properties for the target
   *
   * @return the modified tagret dataset, or the supplementary dataset if the target is null
   */
  @Nullable
  private Dataset merge(@Nullable Dataset target, @Nullable Dataset supplementary) {
    // nothing to merge, return the target (which may be null)
    if (supplementary == null) {
      return target;
    }

    // nothing to overlay into
    if (target == null) {
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

    return target;
  }

  /**
   * Returns the parsed, preferred metadata document as a dataset.
   */
  private Dataset getPreferredMetadataDataset(UUID key) {
    List<Metadata> docs = listMetadata(key, null);
    if (!docs.isEmpty()) {
      InputStream stream = null;
      try {
        // the list is sorted by priority already, just pick the first!
        stream = getMetadataDocument(docs.get(0).getKey());
        return DatasetParser.build(stream);
      } catch (IOException e) {
        LOG.error("Stored metadata document {} cannot be read", docs.get(0).getKey(), e);
      } finally {
        Closeables.closeQuietly(stream);
      }
    }

    return null;
  }

  public List<Metadata> listMetadata(@PathParam("key") UUID datasetKey, @QueryParam("type") MetadataType type) {
    return metadataMapper.list(datasetKey, type);
  }

  public InputStream getMetadataDocument(@PathParam("key") int metadataKey) {
    return new ByteArrayInputStream(metadataMapper.getDocument(metadataKey).getData());
  }


  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, Date date, Date date1) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, String s) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiersUntil(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemIdentifiersResult getItemIdentifiers(List<ScopedFilter> list, int i, int i1, String s, Date date, Date date1) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int i, int i1, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, Date date, Date date1) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, String s) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItemsUntil(List<ScopedFilter> list, int i, int i1, String s, Date date) throws OAIException {
    return null;
  }

  @Override
  public ListItemsResults getItems(List<ScopedFilter> list, int i, int i1, String s, Date date, Date date1) throws OAIException {
    return null;
  }
}
