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
package org.gbif.registry.doi;

import org.gbif.occurrence.query.TitleLookupServiceImpl;
import org.gbif.registry.persistence.mapper.DatasetMapper;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("TitleLookupServiceRegistry")
public class TitleLookupServiceRegistry extends TitleLookupServiceImpl {

  private final DatasetMapper datasetMapper;

  /**
   * Creates a lookup instance from an existing jersey client resource pointing to the root of the
   * API.
   *
   * @param apiRoot
   */
  public TitleLookupServiceRegistry(
      @Value("${api.root.url}") String apiRoot, DatasetMapper datasetMapper) {
    super(apiRoot);
    this.datasetMapper = datasetMapper;
  }

  @Override
@Override
public String getDatasetTitle(String datasetKey) {
  if (datasetKey == null || datasetKey.isBlank()) {
    return datasetKey;
  }

  try {
    String title = datasetMapper.title(UUID.fromString(datasetKey));
    if (title != null && !title.isBlank()) {
      return title;
    }
  } catch (IllegalArgumentException e) {
    // Not a UUID — fall back to the default implementation.
    log.debug("Dataset key is not a UUID: {}", datasetKey);
  } catch (Exception e) {
    log.warn(
        "Cannot lookup dataset title {} in registry DB, falling back to API lookup",
        datasetKey,
        e);
  }

  return super.getDatasetTitle(datasetKey);
}
  }
}
