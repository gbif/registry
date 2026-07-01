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
package org.gbif.registry.ws.resources;

import org.gbif.registry.persistence.mapper.DatasetValidationMapper;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatasetValidationResourceTest {

  private static final String REPORT = "{\"valid\": true}";

  @Mock private DatasetValidationMapper datasetValidationMapper;
  @InjectMocks private DatasetValidationResource resource;

  @Test
  public void testGetReturnsReport() {
    UUID datasetKey = UUID.randomUUID();
    when(datasetValidationMapper.get(datasetKey, 1)).thenReturn(REPORT);

    assertEquals(REPORT, resource.get(datasetKey, 1));
    verify(datasetValidationMapper).get(datasetKey, 1);
  }

  @Test
  public void testGetReturnsNullWhenNotFound() {
    UUID datasetKey = UUID.randomUUID();
    when(datasetValidationMapper.get(datasetKey, 1)).thenReturn(null);

    assertNull(resource.get(datasetKey, 1));
  }

  @Test
  public void testGetLatestReturnsReport() {
    UUID datasetKey = UUID.randomUUID();
    when(datasetValidationMapper.getLatest(datasetKey)).thenReturn(REPORT);

    assertEquals(REPORT, resource.getLatest(datasetKey));
    verify(datasetValidationMapper).getLatest(datasetKey);
  }

  @Test
  public void testGetLatestReturnsNullWhenNotFound() {
    UUID datasetKey = UUID.randomUUID();
    when(datasetValidationMapper.getLatest(datasetKey)).thenReturn(null);

    assertNull(resource.getLatest(datasetKey));
  }

  @Test
  public void testCreateOrUpdateDelegatesToMapper() {
    UUID datasetKey = UUID.randomUUID();

    resource.createOrUpdate(datasetKey, 1, REPORT);

    verify(datasetValidationMapper).createOrUpdate(datasetKey, 1, REPORT);
  }
}
