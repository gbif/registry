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
package org.gbif.registry.ws.client.collections;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.*;
import org.gbif.api.model.collections.descriptors.*;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.*;
import org.gbif.api.model.collections.suggestions.*;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CollectionClient extends BaseCollectionEntityClient<Collection, CollectionChangeSuggestion> {

  @RequestLine("GET /grscicoll/collection")
  @Headers("Accept: application/json")
  PagingResponse<CollectionView> list(@QueryMap CollectionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/collection/listForInstitution")
  @Headers("Accept: application/json")
  PagingResponse<CollectionView> listForInstitutions(@QueryMap InstitutionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/collection/latimerCore")
  @Headers("Accept: application/json")
  PagingResponse<ObjectGroup> listAsLatimerCore(@QueryMap CollectionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/collection/latimerCore/{key}")
  @Headers("Accept: application/json")
  ObjectGroup getAsLatimerCore(@Param("key") UUID key);

  @RequestLine("POST /grscicoll/collection/latimerCore")
  @Headers("Content-Type: application/json")
  UUID createFromLatimerCore(ObjectGroup objectGroup);

  @RequestLine("PUT /grscicoll/collection/latimerCore/{key}")
  @Headers("Content-Type: application/json")
  void updateFromLatimerCore(@Param("key") UUID key, ObjectGroup objectGroup);

  @RequestLine("GET /grscicoll/collection/deleted")
  @Headers("Accept: application/json")
  PagingResponse<CollectionView> listDeleted(@QueryMap CollectionSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/collection/suggest?q={q}")
  @Headers("Accept: application/json")
  List<KeyCodeNameResult> suggest(@Param("q") String q);

  @RequestLine("GET /grscicoll/collection/{key}")
  @Headers("Accept: application/json")
  CollectionView getCollectionView(@Param("key") UUID key);

  @Override
  default Collection get(UUID key) {
    CollectionView view = getCollectionView(key);
    return view != null ? view.getCollection() : null;
  }

  @RequestLine("POST /grscicoll/collection/import")
  @Headers("Content-Type: application/json")
  UUID createFromDataset(CollectionImportParams importParams);

  // Multipart descriptor group creation
  @RequestLine("POST /grscicoll/collection/{collectionKey}/descriptorGroup")
  @Headers("Content-Type: multipart/form-data")
  long createDescriptorGroup(
    @Param("collectionKey") UUID collectionKey,
    @Param("format") ExportFormat format,
    byte[] descriptorsFile,
    @Param("title") @Trim String title,
    @Param("description") @Trim String description,
    @Param("tags") Set<String> tags
  );

  @RequestLine("PUT /grscicoll/collection/{collectionKey}/descriptorGroup/{key}")
  @Headers("Content-Type: multipart/form-data")
  void updateDescriptorGroup(
    @Param("collectionKey") UUID collectionKey,
    @Param("key") long descriptorGroupKey,
    @Param("format") ExportFormat format,
    byte[] descriptorsFile,
    @Param("title") @Trim String title,
    @Param("description") @Trim String description,
    @Param("tags") Set<String> tags
  );

  @RequestLine("GET /grscicoll/collection/{collectionKey}/descriptorGroup/{key}")
  @Headers("Accept: application/json")
  DescriptorGroup getCollectionDescriptorGroup(@Param("collectionKey") UUID collectionKey, @Param("key") long descriptorGroupKey);

  @RequestLine("DELETE /grscicoll/collection/{collectionKey}/descriptorGroup/{key}")
  void deleteCollectionDescriptorGroup(@Param("collectionKey") UUID collectionKey, @Param("key") long descriptorGroupKey);

  @RequestLine("GET /grscicoll/collection/{collectionKey}/descriptorGroup")
  @Headers("Accept: application/json")
  PagingResponse<DescriptorGroup> listCollectionDescriptorGroups(@Param("collectionKey") UUID collectionKey, @QueryMap DescriptorGroupSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/collection/{collectionKey}/descriptorGroup/{key}/descriptor")
  @Headers("Accept: application/json")
  PagingResponse<Descriptor> listCollectionDescriptors(@Param("collectionKey") UUID collectionKey, @Param("key") long descriptorGroupKey, @QueryMap DescriptorSearchRequest searchRequest);

  @RequestLine("GET /grscicoll/collection/{collectionKey}/descriptorGroup/{descriptorGroupKey}/descriptor/{key}")
  @Headers("Accept: application/json")
  Descriptor getCollectionDescriptor(@Param("collectionKey") UUID collectionKey, @Param("descriptorGroupKey") long descriptorGroupKey, @Param("key") long descriptorKey);

  @RequestLine("POST /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion")
  @Headers("Content-Type: multipart/form-data")
  DescriptorChangeSuggestion createDescriptorSuggestion(
    @Param("collectionKey") UUID collectionKey,
    byte[] file,
    @Param("type") Type type,
    @Param("title") String title,
    @Param("description") String description,
    @Param("format") ExportFormat format,
    @Param("comments") List<String> comments,
    @Param("proposerEmail") String proposerEmail,
    @Param("tags") Set<String> tags
  );

  @RequestLine("GET /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion/{key}")
  DescriptorChangeSuggestion getDescriptorSuggestion(@Param("collectionKey") UUID collectionKey, @Param("key") long key);

  @RequestLine("PUT /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion/{key}/apply")
  void applyDescriptorSuggestion(@Param("collectionKey") UUID collectionKey, @Param("key") long key);

  @RequestLine("PUT /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion/{key}/discard")
  void discardDescriptorSuggestion(@Param("collectionKey") UUID collectionKey, @Param("key") long key);

  @RequestLine("DELETE /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion/{key}")
  void deleteDescriptorSuggestion(@Param("collectionKey") UUID collectionKey, @Param("key") long key);

  @RequestLine("PUT /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion/{key}")
  @Headers("Content-Type: multipart/form-data")
  void updateDescriptorSuggestion(
    @Param("collectionKey") UUID collectionKey,
    @Param("key") long key,
    byte[] file,
    @Param("type") Type type,
    @Param("title") String title,
    @Param("description") String description,
    @Param("format") ExportFormat format,
    @Param("comments") List<String> comments,
    @Param("proposerEmail") String proposerEmail,
    @Param("tags") Set<String> tags
  );

  @RequestLine("GET /grscicoll/collection/{collectionKey}/descriptorGroup/suggestion")
  @Headers("Accept: application/json")
  PagingResponse<DescriptorChangeSuggestion> listDescriptorSuggestions(
    @Param("collectionKey") UUID collectionKey,
    @Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @QueryMap Pageable page
  );

  @RequestLine("GET /grscicoll/collection/descriptorGroup/suggestion")
  @Headers("Accept: application/json")
  PagingResponse<DescriptorChangeSuggestion> listAllDescriptorSuggestions(
    @Param("status") Status status,
    @Param("type") Type type,
    @Param("proposerEmail") String proposerEmail,
    @QueryMap Pageable page
  );
}
