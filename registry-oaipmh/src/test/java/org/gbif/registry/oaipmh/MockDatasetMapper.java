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
package org.gbif.registry.oaipmh;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Grid;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.Country;
import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.params.DatasetListParams;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.apache.ibatis.annotations.Param;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/** Mock DatasetMapper that works with in memory lists for testings purpose. */
public class MockDatasetMapper implements DatasetMapper {

  private Set<Country> distinctCountries = Sets.newHashSet();
  private Set<Installation> distinctInstallation = Sets.newHashSet();

  private Map<UUID, Long> datasetsByInstallation = Maps.newHashMap();
  private Map<Country, Long> datasetsByCountry = Maps.newHashMap();

  /**
   * Mock specific method Warning: this method will add the country as a distinct country and
   * increment the dataset counts for that country
   *
   * @param country
   */
  public void mockDatasetForCountry(Country country) {
    distinctCountries.add(country);
    if (datasetsByCountry.containsKey(country)) {
      datasetsByCountry.put(country, datasetsByCountry.get(country) + 1);
    } else {
      datasetsByCountry.put(country, 1l);
    }
  }

  /**
   * Mock specific method Warning: this method sets datasetsByInstallation to 1 for the specified
   * country
   *
   * @param installation
   */
  public void mockDatasetForInstallation(Installation installation) {
    distinctInstallation.add(installation);
    UUID installationKey = installation.getKey();
    if (datasetsByInstallation.containsKey(installationKey)) {
      datasetsByInstallation.put(installationKey, datasetsByInstallation.get(installationKey) + 1);
    } else {
      datasetsByInstallation.put(installationKey, 1l);
    }
  }

  @Override
  public List<Dataset> listDatasetsHostedBy(
      @Param("organizationKey") UUID organizationKey, @Nullable @Param("page") Pageable page) {
    return null;
  }

  @Override
  public List<Dataset> listDatasetsEndorsedBy(
      @Param("nodeKey") UUID nodeKey, @Nullable @Param("page") Pageable page) {
    return null;
  }

  @Override
  public List<Dataset> list(@Nullable @Param("params") DatasetListParams params) {
    return null;
  }

  @Override
  public long count(@Nullable @Param("params") DatasetListParams params) {
    if (params.getCountry() != null) {
      return datasetsByCountry.getOrDefault(params.getCountry(), 0L);
    }
    if (params.getInstallationKey() != null) {
      Long count = datasetsByInstallation.getOrDefault(params.getInstallationKey(), 0L);
      return count == null ? 0 : count;
    }
    return 0;
  }

  @Override
  public long countDatasetsEndorsedBy(@Param("nodeKey") UUID nodeKey) {
    return 0;
  }

  @Override
  public long countDatasetsHostedBy(@Param("organizationKey") UUID organizationKey) {
    return 0;
  }

  @Override
  public List<Dataset> withNoEndpoint(@Nullable @Param("page") Pageable page) {
    return null;
  }

  @Override
  public long countWithNoEndpoint() {
    return 0;
  }

  @Override
  public List<Country> listDistinctCountries(@Nullable @Param("page") Pageable page) {
    return Lists.newArrayList(distinctCountries);
  }

  @Override
  public List<Installation> listDistinctInstallations(@Nullable @Param("page") Pageable page) {
    return Lists.newArrayList(distinctInstallation);
  }

  @Override
  public int addComment(
      @Param("targetEntityKey") UUID entityKey, @Param("commentKey") int commentKey) {
    return 0;
  }

  @Override
  public int deleteComment(
      @Param("targetEntityKey") UUID entityKey, @Param("commentKey") int commentKey) {
    return 0;
  }

  @Override
  public List<Comment> listComments(@Param("targetEntityKey") UUID identifierKey) {
    return null;
  }

  @Override
  public int addContact(
      @Param("targetEntityKey") UUID entityKey,
      @Param("contactKey") int contactKey,
      @Param("type") ContactType contactType,
      @Param("isPrimary") boolean isPrimary) {
    return 0;
  }

  @Override
  public void updatePrimaryContacts(
      @Param("targetEntityKey") UUID entityKey, @Param("type") ContactType contactType) {}

  @Override
  public void updateContact(
      @Param("targetEntityKey") UUID entityKey,
      @Param("contactKey") Integer contactKey,
      @Param("type") ContactType contactType,
      @Param("primary") boolean primary) {}

  @Override
  public int deleteContact(
      @Param("targetEntityKey") UUID entityKey, @Param("contactKey") int contactKey) {
    return 0;
  }

  @Override
  public int deleteContacts(@Param("targetEntityKey") UUID entityKey) {
    return 0;
  }

  @Override
  public List<Contact> listContacts(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public Boolean areRelated(
      @Param("targetEntityKey") UUID targetEntityKey, @Param("contactKey") int contactKey) {
    return null;
  }

  @Override
  public int addEndpoint(
      @Param("targetEntityKey") UUID entityKey, @Param("endpointKey") int endpointKey) {
    return 0;
  }

  @Override
  public int deleteEndpoint(
      @Param("targetEntityKey") UUID entityKey, @Param("endpointKey") int endpointKey) {
    return 0;
  }

  @Override
  public List<Endpoint> listEndpoints(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public int addIdentifier(
      @Param("targetEntityKey") UUID entityKey, @Param("identifierKey") int identifierKey) {
    return 0;
  }

  @Override
  public int deleteIdentifier(
      @Param("targetEntityKey") UUID entityKey, @Param("identifierKey") int identifierKey) {
    return 0;
  }

  @Override
  public List<Identifier> listIdentifiers(@Param("targetEntityKey") UUID identifierKey) {
    return null;
  }

  @Override
  public int addMachineTag(
      @Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey) {
    return 0;
  }

  @Override
  public int deleteMachineTag(
      @Param("targetEntityKey") UUID entityKey, @Param("machineTagKey") int machineTagKey) {
    return 0;
  }

  @Override
  public int deleteMachineTags(
      @Param("targetEntityKey") UUID entityKey,
      @Param("namespace") String namespace,
      @Param("name") String name) {
    return 0;
  }

  @Override
  public List<MachineTag> listMachineTags(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public Dataset get(@Param("key") UUID key) {
    return null;
  }

  @Override
  public boolean exists(UUID key) {
    return key != null;
  }

  @Override
  public String title(@Param("key") UUID key) {
    return null;
  }

  @Override
  public void create(Dataset entity) {}

  @Override
  public void delete(@Param("key") UUID key) {}

  @Override
  public void update(Dataset entity) {}

  @Override
  public int addTag(@Param("targetEntityKey") UUID entityKey, @Param("tagKey") int tagKey) {
    return 0;
  }

  @Override
  public int deleteTag(@Param("targetEntityKey") UUID entityKey, @Param("tagKey") int tagKey) {
    return 0;
  }

  @Override
  public List<Tag> listTags(@Param("targetEntityKey") UUID targetEntityKey) {
    return null;
  }

  @Override
  public List<Grid> listGrids(UUID datasetKey) {
    return null;
  }

  @Override
  public Dataset getLightweight(UUID key) {
    return null;
  }

  @Override
  public void createDwcaDataset(UUID datasetKey, Dataset.DwcA dwcaDataset) {
  }

  @Override
  public void updateDwcaDataset(UUID datasetKey, Dataset.DwcA dwcaDataset) {
  }

  @Override
  public List<Dataset> findDatasetsWithDeprecatedCategories(Set<String> deprecatedCategories) {
    return List.of();
  }
}
