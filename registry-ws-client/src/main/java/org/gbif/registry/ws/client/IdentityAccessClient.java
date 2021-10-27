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

import org.gbif.api.model.common.GbifUser;
import org.gbif.api.service.common.IdentityAccessService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public interface IdentityAccessClient extends IdentityAccessService {

  @RequestMapping(
      method = RequestMethod.GET,
      value = "admin/user/{username}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  @Override
  GbifUser get(@PathVariable("username") String username);

  @Override
  default GbifUser authenticate(String username, String password) {
    throw new UnsupportedOperationException("Not implemented in Ws Client");
  }
}
