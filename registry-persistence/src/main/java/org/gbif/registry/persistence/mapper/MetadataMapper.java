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
package org.gbif.registry.persistence.mapper;

import org.gbif.api.model.registry.Metadata;
import org.gbif.api.vocabulary.MetadataType;
import org.gbif.registry.persistence.mapper.handler.ByteArrayWrapper;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

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

  /** Stores a new metadata document with its source document as a byte array exactly as it was. */
  int create(@Param("meta") Metadata metadata, @Param("data") byte[] content);

  void delete(@Param("key") int key);

  /**
   * Return all metadata entries for a given dataset key ordered by priority and creation date, i.e.
   * first come the EML documents ordered by creation, then the Dublin Core ones.
   *
   * @param datasetKey the dataset key the returned metadata belongs to
   * @param type optional metadata type to filter
   */
  List<Metadata> list(@Param("key") UUID datasetKey, @Param("type") @Nullable MetadataType type);
}
