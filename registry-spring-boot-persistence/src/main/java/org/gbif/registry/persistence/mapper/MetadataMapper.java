package org.gbif.registry.persistence.mapper;

import org.apache.ibatis.annotations.Param;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.persistence.mapper.handler.ByteArrayWrapper;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@Repository
public interface MetadataMapper {
  /**
   * This gets the instance in question.
   *
   * @param key of the metadata record to fetch
   * @return either the requested metadata or {@code null} if it couldn't be found
   */
  Metadata get(@Param("key") int key);

  /**
   * Return the content of a metadata entry.
   *
   * @param key of the metadata record to fetch
   * @return either the requested metadata or {@code null} if it couldn't be found
   */
  ByteArrayWrapper getDocument(@Param("key") int key);

  /**
   * Stores a new metadata document with its source document as a byte array exactly as it was.
   */
  int create(@Param("meta") Metadata metadata, @Param("data") byte[] content);

  void delete(@Param("key") int key);

  /**
   * Return all metadata entries for a given dataset key ordered by priority and creation date, i.e. first come the
   * EML documents ordered by creation, then the Dublin Core ones.
   * @param datasetKey the dataset key the returned metadata belongs to
   * @param type optional metadata type to filter
   */
  List<Metadata> list(@Param("key") UUID datasetKey, @Param("type") @Nullable MetadataType type);

}
