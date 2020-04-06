/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.MetadataType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("dataset")
public interface DatasetClient extends NetworkEntityClient<Dataset>, DatasetService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/constituents",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listConstituents(
      @PathVariable("key") UUID key, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "constituents",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listConstituents(@SpringQueryMap Pageable pageable);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listByCountry(
      @RequestParam("country") Country country,
      @RequestParam(value = "type", required = false) DatasetType datasetType,
      @SpringQueryMap Pageable pageable);

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listByType(
      @RequestParam("type") DatasetType datasetType, @SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/metadata",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Metadata> listMetadata(
      @PathVariable("key") UUID key,
      @RequestParam(value = "type", required = false) MetadataType type);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}/networks",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  List<Network> listNetworks(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "metadata/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  Metadata getMetadata(@PathVariable("key") int key);

  @RequestMapping(method = RequestMethod.DELETE, value = "metadata/{key}")
  @Override
  void deleteMetadata(@PathVariable("key") int key);

  @Override
  default Metadata insertMetadata(UUID key, InputStream document) {
    try {
      return insertMetadata(key, IOUtils.toByteArray(document));
    } catch (IOException e) {
      throw new IllegalArgumentException("Unreadable document", e);
    }
  }

  @RequestMapping(
      method = RequestMethod.POST,
      value = "{key}/document",
      consumes = MediaType.APPLICATION_XML_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  Metadata insertMetadata(@PathVariable("key") UUID key, @RequestBody byte[] bytes);

  @Override
  default InputStream getMetadataDocument(UUID key) {
    byte[] bytes = getMetadataDocumentAsBytes(key);

    if (bytes != null) {
      return new ByteArrayInputStream(bytes);
    }

    return null;
  }

  @RequestMapping(method = RequestMethod.GET, value = "{key}/document")
  @ResponseBody
  byte[] getMetadataDocumentAsBytes(@PathVariable("key") UUID key);

  @Override
  default InputStream getMetadataDocument(int key) {
    return new ByteArrayInputStream(getMetadataDocumentAsBytes(key));
  }

  @RequestMapping(method = RequestMethod.GET, value = "metadata/{key}/document")
  @ResponseBody
  byte[] getMetadataDocumentAsBytes(@PathVariable("key") int key);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "deleted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listDeleted(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "duplicate",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listDuplicates(@SpringQueryMap Pageable pageable);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "withNoEndpoint",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  PagingResponse<Dataset> listDatasetsWithNoEndpoint(@SpringQueryMap Pageable pageable);

  @Override
  default PagingResponse<Dataset> listByDOI(String doi, Pageable pageable) {
    DOI doiObject = new DOI(doi);
    return listByDOI(doiObject.getPrefix(), doiObject.getSuffix(), pageable);
  }

  @RequestMapping(
      method = RequestMethod.GET,
      value = "doi/{prefix}/{suffix}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<Dataset> listByDOI(
      @PathVariable("prefix") String prefix,
      @PathVariable("suffix") String suffix,
      @SpringQueryMap Pageable pageable);
}
