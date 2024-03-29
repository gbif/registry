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
package org.gbif.registry.service.collections.duplicates;

import org.gbif.api.model.collections.duplicates.DuplicatesResult;
import org.gbif.registry.persistence.mapper.collections.DuplicatesMapper;
import org.gbif.registry.persistence.mapper.collections.params.DuplicatesSearchParams;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionDuplicatesService extends BaseDuplicatesService {

  private final DuplicatesMapper duplicatesMapper;

  @Autowired
  public CollectionDuplicatesService(DuplicatesMapper duplicatesMapper) {
    this.duplicatesMapper = duplicatesMapper;
  }

  @Override
  public DuplicatesResult findPossibleDuplicates(DuplicatesSearchParams params) {
    return processDBResults(
        duplicatesMapper::getCollectionDuplicates,
        duplicatesMapper::getCollectionsMetadata,
        params);
  }
}
