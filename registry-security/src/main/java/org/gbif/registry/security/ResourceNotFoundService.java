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
package org.gbif.registry.security;

import org.gbif.registry.persistence.mapper.DatasetMapper;
import org.gbif.registry.persistence.mapper.InstallationMapper;
import org.gbif.registry.persistence.mapper.NetworkMapper;
import org.gbif.registry.persistence.mapper.NodeMapper;
import org.gbif.registry.persistence.mapper.OrganizationMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;

/**
 * This service provides the necessary methods for the {@link ResourceNotFoundRequestFilter}. It had
 * to be done in this class to be able to mock it in the tests.
 */
@Service
public class ResourceNotFoundService {

  private static final Map<Resource, Predicate<UUID>> ENTITY_EXISTS_PREDICATES = new HashMap<>();

  public ResourceNotFoundService(
      OrganizationMapper organizationMapper,
      DatasetMapper datasetMapper,
      InstallationMapper installationMapper,
      NodeMapper nodeMapper,
      NetworkMapper networkMapper,
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper) {
    ENTITY_EXISTS_PREDICATES.put(Resource.ORGANIZATION, organizationMapper::exists);
    ENTITY_EXISTS_PREDICATES.put(Resource.DATASET, datasetMapper::exists);
    ENTITY_EXISTS_PREDICATES.put(Resource.INSTALLATION, installationMapper::exists);
    ENTITY_EXISTS_PREDICATES.put(Resource.NODE, nodeMapper::exists);
    ENTITY_EXISTS_PREDICATES.put(Resource.NETWORK, networkMapper::exists);
    ENTITY_EXISTS_PREDICATES.put(Resource.INSTITUTION, institutionMapper::exists);
    ENTITY_EXISTS_PREDICATES.put(Resource.COLLECTION, collectionMapper::exists);
  }

  public boolean entityExists(Resource resource, UUID key) {
    return ENTITY_EXISTS_PREDICATES.get(resource).test(key);
  }

  public enum Resource {
    ORGANIZATION,
    DATASET,
    INSTALLATION,
    NODE,
    NETWORK,
    INSTITUTION,
    COLLECTION;

    public static Resource fromString(String type) {
      try {
        return Resource.valueOf(type.toUpperCase());
      } catch (Exception ex) {
        return null;
      }
    }
  }
}
