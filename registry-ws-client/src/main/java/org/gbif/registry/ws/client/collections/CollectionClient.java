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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionImportParams;
import org.gbif.api.model.collections.latimercore.ObjectGroup;
import org.gbif.api.model.collections.request.CollectionSearchRequest;
import org.gbif.api.model.collections.suggestions.CollectionChangeSuggestion;
import org.gbif.api.model.collections.view.CollectionView;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RequestMapping("grscicoll/collection")
public interface CollectionClient
    extends BaseCollectionEntityClient<Collection, CollectionChangeSuggestion> {

  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  PagingResponse<CollectionView> list(@SpringQueryMap CollectionSearchRequest searchRequest);

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
}
