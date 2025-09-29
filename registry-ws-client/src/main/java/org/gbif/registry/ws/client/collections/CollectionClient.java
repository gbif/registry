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

import org.gbif.api.annotation.Trim;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorChangeSuggestion;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.collections.request.InstitutionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.suggestions.Status;
import org.gbif.api.model.collections.suggestions.Type;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("grscicoll/collection")
public interface CollectionClient
    extends BaseCollectionEntityClient<Collection, CollectionChangeSuggestion> {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<CollectionView> list(@SpringQueryMap CollectionSearchRequest searchRequest);

  @GetMapping(value = "listForInstitution", produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<CollectionView> listForInstitutions(@SpringQueryMap InstitutionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "latimerCore",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<ObjectGroup> listAsLatimerCore(
      @SpringQueryMap CollectionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "latimerCore/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  ObjectGroup getAsLatimerCore(@PathVariable("key") UUID key);

  @RequestMapping(
      method = RequestMethod.POST,
      value = "latimerCore",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  UUID createFromLatimerCore(@RequestBody ObjectGroup objectGroup);

  @RequestMapping(
      method = RequestMethod.PUT,
      value = "latimerCore/{key}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  void updateFromLatimerCore(@PathVariable("key") UUID key, @RequestBody ObjectGroup objectGroup);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "deleted",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<CollectionView> listDeleted(@SpringQueryMap CollectionSearchRequest searchRequest);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "suggest",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  CollectionView getCollectionView(@PathVariable("key") UUID key);

  @Override
  default Collection get(UUID key) {
    CollectionView view = getCollectionView(key);
    return view != null ? view.getCollection() : null;
  }

  @RequestMapping(
      method = RequestMethod.POST,
      value = "import",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  UUID createFromDataset(@RequestBody CollectionImportParams importParams);

  @PostMapping(
      value = "{collectionKey}/descriptorGroup",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  long createDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @RequestParam(value = "format", defaultValue = "CSV") ExportFormat format,
      @RequestPart("descriptorsFile") MultipartFile descriptorsFile,
      @RequestParam("title") @Trim String title,
      @RequestParam(value = "description", required = false) @Trim String description,
      @RequestParam(value = "tags", required = false) Set<String> tags);

  @PutMapping(
      value = "{collectionKey}/descriptorGroup/{key}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  void updateDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey,
      @RequestParam(value = "format", defaultValue = "CSV") ExportFormat format,
      @RequestPart("descriptorsFile") MultipartFile descriptorsFile,
      @RequestParam("title") @Trim String title,
      @RequestParam(value = "description", required = false) @Trim String description,
      @RequestParam(value = "tags", required = false) Set<String> tags);

  @GetMapping(
      value = "{collectionKey}/descriptorGroup/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  DescriptorGroup getCollectionDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey);

  @DeleteMapping(value = "{collectionKey}/descriptorGroup/{key}")
  void deleteCollectionDescriptorGroup(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey);

  @GetMapping(
      value = "{collectionKey}/descriptorGroup",
      produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<DescriptorGroup> listCollectionDescriptorGroups(
      @PathVariable("collectionKey") UUID collectionKey,
      @SpringQueryMap DescriptorGroupSearchRequest searchRequest);

  @GetMapping(
      value = "{collectionKey}/descriptorGroup/{key}/descriptor",
      produces = MediaType.APPLICATION_JSON_VALUE)
  PagingResponse<Descriptor> listCollectionDescriptors(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long descriptorGroupKey,
      @SpringQueryMap DescriptorSearchRequest searchRequest);

  @GetMapping(
      value = "{collectionKey}/descriptorGroup/{descriptorGroupKey}/descriptor/{key}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  Descriptor getCollectionDescriptor(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("descriptorGroupKey") long descriptorGroupKey,
      @PathVariable("key") long descriptorKey);

  @PostMapping(value = "{collectionKey}/descriptorGroup/suggestion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  DescriptorChangeSuggestion createDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @RequestPart("file") MultipartFile file,
      @RequestParam("type") Type type,
      @RequestParam("title") String title,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam("format") ExportFormat format,
      @RequestParam("comments") List<String> comments,
      @RequestParam("proposerEmail") String proposerEmail,
      @RequestParam(value = "tags", required = false) Set<String> tags);

  @GetMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}")
  DescriptorChangeSuggestion getDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key);

  @PutMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}/apply")
  void applyDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key);

  @PutMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}/discard")
  void discardDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key);

  @DeleteMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}")
  void deleteDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key);

  @PutMapping(value = "{collectionKey}/descriptorGroup/suggestion/{key}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  void updateDescriptorSuggestion(
      @PathVariable("collectionKey") UUID collectionKey,
      @PathVariable("key") long key,
      @RequestPart(value = "file", required = false) MultipartFile file,
      @RequestParam("type") Type type,
      @RequestParam("title") String title,
      @RequestParam("description") String description,
      @RequestParam("format") ExportFormat format,
      @RequestParam("comments") List<String> comments,
      @RequestParam("proposerEmail") String proposerEmail,
      @RequestParam(value = "tags", required = false) Set<String> tags);

  @GetMapping(value = "{collectionKey}/descriptorGroup/suggestion")
  PagingResponse<DescriptorChangeSuggestion> listDescriptorSuggestions(
      @PathVariable("collectionKey") UUID collectionKey,
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @SpringQueryMap Pageable page);

  @GetMapping(value = "descriptorGroup/suggestion")
  PagingResponse<DescriptorChangeSuggestion> listAllDescriptorSuggestions(
      @RequestParam(value = "status", required = false) Status status,
      @RequestParam(value = "type", required = false) Type type,
      @RequestParam(value = "proposerEmail", required = false) String proposerEmail,
      @SpringQueryMap Pageable page);
}
