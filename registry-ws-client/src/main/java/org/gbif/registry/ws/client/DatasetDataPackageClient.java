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
package org.gbif.registry.ws.client;

import io.swagger.v3.oas.annotations.Hidden;

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.search.DatasetRequestSearchParams;
import org.gbif.api.service.registry.DatasetDataPackageService;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.MetadataType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("dataset")
public interface DatasetDataPackageClient extends DatasetDataPackageService {


  @PostMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void createDataPackageData(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage);

  @PutMapping(value = "{datasetKey}/datapackage", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  void updateDataPackageData(@PathVariable("datasetKey") UUID datasetKey, @RequestBody @Trim Dataset.DataPackage dataPackage);

  @GetMapping(value = "{datasetKey}/datapackage", produces = MediaType.APPLICATION_JSON_VALUE)
  @Override
  Dataset.DataPackage getDataPackageData(@PathVariable("datasetKey") UUID datasetKey);

}
