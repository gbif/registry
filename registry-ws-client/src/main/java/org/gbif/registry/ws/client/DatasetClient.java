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
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Metadata;
import org.gbif.api.model.registry.Network;
import org.gbif.api.model.registry.search.DatasetRequestSearchParams;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.MetadataType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface DatasetClient
  extends NetworkEntityClient<Dataset>, DatasetService {

  // ---------------------------------------------------------------------------
  // Constituents
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset/{key}/constituents")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listConstituents(
    @Param("key") UUID key,
    @QueryMap Pageable pageable);

  @Override
  @RequestLine("GET /dataset/constituents")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listConstituents(
    @QueryMap Pageable pageable);

  // ---------------------------------------------------------------------------
  // Listing
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset?country={country}&type={type}")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listByCountry(
    @Param("country") Country country,
    @Param("type") DatasetType datasetType,
    @QueryMap Pageable pageable);

  @Override
  @RequestLine("GET /dataset?type={type}")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listByType(
    @Param("type") DatasetType datasetType,
    @QueryMap Pageable pageable);

  @Override
  @RequestLine("GET /dataset")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> list(
    @QueryMap DatasetRequestSearchParams searchParams);

  // ---------------------------------------------------------------------------
  // Metadata
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset/{key}/metadata?type={type}")
  @Headers("Accept: application/json")
  List<Metadata> listMetadata(
    @Param("key") UUID key,
    @Param("type") MetadataType type);

  @Override
  @RequestLine("GET /dataset/{key}/networks")
  @Headers("Accept: application/json")
  List<Network> listNetworks(
    @Param("key") UUID key);

  @Override
  @RequestLine("GET /dataset/metadata/{key}")
  @Headers("Accept: application/json")
  Metadata getMetadata(
    @Param("key") int key);

  @Override
  @RequestLine("DELETE /dataset/metadata/{key}")
  void deleteMetadata(
    @Param("key") int key);

  // ---------------------------------------------------------------------------
  // Metadata documents
  // ---------------------------------------------------------------------------

  @Override
  default Metadata insertMetadata(UUID key, InputStream document) {
    try {
      return insertMetadata(key, toByteArray(document));
    } catch (IOException e) {
      throw new IllegalArgumentException("Unreadable document", e);
    }
  }

  static byte[] toByteArray(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    byte[] data = new byte[4096]; // 4 KB buffer
    int nRead;
    while ((nRead = input.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, nRead);
    }
    buffer.flush();
    return buffer.toByteArray();
  }


  @RequestLine("POST /dataset/{key}/document")
  @Headers({
    "Content-Type: application/xml",
    "Accept: application/json"
  })
  Metadata insertMetadata(
    @Param("key") UUID key,
    byte[] bytes);

  @Override
  default InputStream getMetadataDocument(UUID key) {
    byte[] bytes = getMetadataDocumentAsBytes(key);
    return bytes != null ? new ByteArrayInputStream(bytes) : null;
  }

  @RequestLine("GET /dataset/{key}/document")
  byte[] getMetadataDocumentAsBytes(
    @Param("key") UUID key);

  @Override
  default InputStream getMetadataDocument(int key) {
    return new ByteArrayInputStream(getMetadataDocumentAsBytes(key));
  }

  @RequestLine("GET /dataset/metadata/{key}/document")
  byte[] getMetadataDocumentAsBytes(
    @Param("key") int key);

  // ---------------------------------------------------------------------------
  // Special listings
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("GET /dataset/deleted")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listDeleted(
    @QueryMap DatasetRequestSearchParams searchParams);

  @Override
  @RequestLine("GET /dataset/duplicate")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listDuplicates(
    @QueryMap Pageable pageable);

  @Override
  @RequestLine("GET /dataset/withNoEndpoint")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listDatasetsWithNoEndpoint(
    @QueryMap Pageable pageable);

  // ---------------------------------------------------------------------------
  // DOI lookup
  // ---------------------------------------------------------------------------

  @Override
  default PagingResponse<Dataset> listByDOI(String doi, Pageable pageable) {
    DOI doiObject = new DOI(doi);
    return listByDOI(doiObject.getPrefix(), doiObject.getSuffix(), pageable);
  }

  @RequestLine("GET /dataset/doi/{prefix}/{suffix}")
  @Headers("Accept: application/json")
  PagingResponse<Dataset> listByDOI(
    @Param("prefix") String prefix,
    @Param("suffix") String suffix,
    @QueryMap Pageable pageable);

  // ---------------------------------------------------------------------------
  // DwC-A metadata
  // ---------------------------------------------------------------------------

  @Override
  @RequestLine("POST /dataset/{datasetKey}/dwca")
  @Headers("Content-Type: application/json")
  void createDwcaData(
    @Param("datasetKey") UUID datasetKey,
    Dataset.DwcA dwcA);

  @Override
  @RequestLine("PUT /dataset/{datasetKey}/dwca")
  @Headers("Content-Type: application/json")
  void updateDwcaData(
    @Param("datasetKey") UUID datasetKey,
    Dataset.DwcA dwcA);
}
