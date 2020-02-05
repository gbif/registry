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
package org.gbif.registry.ws.resources;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.gbif.registry.ws.security.UserRoles.ADMIN_ROLE;
import static org.gbif.registry.ws.security.UserRoles.APP_ROLE;

// for testing
@RestController
@RequestMapping("/test")
public class TestResource {

  @RequestMapping(method = RequestMethod.GET)
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> getWithAdminRoleOnlyAndEmptyRequestResponse() {
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(method = RequestMethod.POST)
  @Secured(ADMIN_ROLE)
  public ResponseEntity<Void> postWithAdminRoleOnlyAndEmptyRequestResponse() {
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PostMapping("/app")
  @Secured(APP_ROLE)
  public ResponseEntity<Void> postWithAppRoleOnlyAndEmptyRequestResponse() {
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @PostMapping("/app2")
  @Secured(APP_ROLE)
  public ResponseEntity<String> postWithAppRoleOnlyAndRequestBodyAndResponse(
      @RequestBody final TestRequest testRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(testRequest.getValue());
  }

  public static class TestRequest {

    private String value;

    public TestRequest() {}

    public TestRequest(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
