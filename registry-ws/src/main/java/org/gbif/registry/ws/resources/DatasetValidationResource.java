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

import org.gbif.api.annotation.Experimental;
import org.gbif.api.service.registry.DatasetValidationService;
import org.gbif.registry.persistence.mapper.DatasetValidationMapper;

import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Dataset Validation Report",
    description = "APIs for storing and retrieving DwC-DP validation reports for datasets.",
    extensions =
    @Extension(
        name = "Order",
        properties = @ExtensionProperty(name = "Order", value = "0101")))
@Validated
@Primary
@RestController
@RequestMapping(value = "dataset", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@Experimental
public class DatasetValidationResource implements DatasetValidationService {

  private final DatasetValidationMapper datasetValidationMapper;

  public DatasetValidationResource(DatasetValidationMapper datasetValidationMapper) {
    this.datasetValidationMapper = datasetValidationMapper;
  }

  @Hidden
  @PutMapping(value = "{datasetKey}/validationreport/{attempt}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void createOrUpdate(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt,
      @RequestBody String report) {
    datasetValidationMapper.createOrUpdate(datasetKey, attempt, report);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/validationreport/{attempt}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public String get(
      @PathVariable("datasetKey") UUID datasetKey,
      @PathVariable("attempt") int attempt) {
    return datasetValidationMapper.get(datasetKey, attempt);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/validationreport", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  public String getLatest(@PathVariable("datasetKey") UUID datasetKey) {
    return datasetValidationMapper.getLatest(datasetKey);
  }
}
