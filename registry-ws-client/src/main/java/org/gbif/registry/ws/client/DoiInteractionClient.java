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

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.registry.doi.DoiInteractionService;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.domain.doi.DoiType;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("doi")
public interface DoiInteractionClient extends DoiInteractionService {

  @RequestMapping(method = RequestMethod.POST, value = "gen/{type}")
  @Override
  DOI generate(@PathVariable("type") DoiType type);

  @RequestMapping(
      method = RequestMethod.GET,
      value = "{prefix}/{suffix}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  DoiData get(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix);

  @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  DOI register(@RequestBody DoiRegistration doiRegistration);

  @RequestMapping(method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
  @Override
  DOI update(@RequestBody DoiRegistration doiRegistration);

  @RequestMapping(method = RequestMethod.DELETE, value = "{prefix}/{suffix}")
  @Override
  void delete(@PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix);
}
