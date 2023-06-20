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

import org.gbif.api.model.registry.Node;
import org.gbif.api.model.registry.search.KeyTitleResult;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.params.InstallationListParams;
import org.gbif.registry.persistence.mapper.params.NodeListParams;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * For simplicity we keep ContactableMapper part of the BaseNetworkEntityMapper, but this NodeMapper
 * does not implement those mapper methods but will throw exceptions instead ! For a Node all
 * contacts are managed in the GBIF Directory which we only access for reads and cannot manipulate
 * though our Java API.
 */
@Repository
public interface NodeMapper extends BaseNetworkEntityMapper<Node> {

  List<Node> list(@Param("params") NodeListParams params);

  long count(@Param("params") NodeListParams params);

  List<Country> listNodeCountries();

  List<Country> listActiveCountries();

  Node getByCountry(@Param("country") Country country);

  /**
   * This method is not supported by the NodeMapper.
   *
   * @throws
   */
  @Override
  int addContact(UUID entityKey, int contactKey, ContactType contactType, boolean isPrimary);

  /**
   * This method is not supported by the NodeMapper.
   *
   * @throws
   */
  @Override
  void updatePrimaryContacts(UUID entityKey, ContactType contactType);

  /**
   * This method is not supported by the NodeMapper.
   *
   * @throws
   */
  @Override
  int deleteContact(UUID entityKey, int contactKey);

  /** A simple suggest by title service. */
  List<KeyTitleResult> suggest(@Nullable @Param("q") String q);
}
