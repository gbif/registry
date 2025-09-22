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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.gbif.api.annotation.Experimental;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.common.paging.PageableBase;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetDataPackageService;
import org.gbif.registry.persistence.mapper.*;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

@SuppressWarnings("UnstableApiUsage")
@io.swagger.v3.oas.annotations.tags.Tag(
    name = "Dataset Data Package",
    description =
        "APIs for managing Dataset Data Package information. Requires ADMIN role.",
    extensions =
        @Extension(
            name = "Order",
            properties = @ExtensionProperty(name = "Order", value = "0100")))
@Validated
@Primary
@RestController
@RequestMapping(value = "dataset", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
@Experimental
public class DatasetDataPackageResource implements DatasetDataPackageService {

  private final DatasetDataPackageMapper datasetDataPackageMapper;
  private final ObjectMapper mapper;

  public DatasetDataPackageResource(DatasetDataPackageMapper datasetDataPackageMapper, ObjectMapper mapper) {
    this.datasetDataPackageMapper = datasetDataPackageMapper;
    this.mapper = mapper;
  }

  @Hidden
  @PostMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void create(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage) {
    datasetDataPackageMapper.create(datasetKey, dataPackage);
  }

  @Hidden
  @PutMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Transactional
  @Secured(ADMIN_ROLE)
  @Override
  public void update(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage) {
    datasetDataPackageMapper.update(datasetKey, dataPackage);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/datapackage", produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Override
  public Dataset.DataPackage get(@PathVariable("datasetKey") UUID datasetKey) {
    return datasetDataPackageMapper.get(datasetKey);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/datapackage/resource", produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Override
  public String getResources(@PathVariable("datasetKey") UUID datasetKey) {
    return datasetDataPackageMapper.getResources(datasetKey);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/datapackage/resource/{resourceName}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Override
  public String getResource(@PathVariable("datasetKey") UUID datasetKey, @PathVariable("resourceName") String resourceName) {
    return datasetDataPackageMapper.getResource(datasetKey, resourceName);
  }

  @Hidden
  @GetMapping(value = "{datasetKey}/datapackage/resourceNames", produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Override
  public List<String> getResourceNames(@PathVariable("datasetKey") UUID datasetKey) {
    return toListOfStrings(datasetDataPackageMapper.getResourceField(datasetKey, "name"));
  }

  @Hidden
  @GetMapping(value = "/datapackage", produces = MediaType.APPLICATION_JSON_VALUE)
  @Trim
  @Override
  public PagingResponse<Dataset.DataPackage> list(PageableBase params) {
    return new PagingResponse<>(params,datasetDataPackageMapper.count(),datasetDataPackageMapper.list(params));
  }

  @SneakyThrows
  private List<String> toListOfStrings(String jsonObject) {
    List<String> result = new ArrayList<>();
    if (jsonObject != null) {
      ArrayNode jsonNode = (ArrayNode) mapper.readTree(jsonObject);
      for (JsonNode node : jsonNode) {
        result.add(node.asText());
      }
    }
    return result;
  }
}

