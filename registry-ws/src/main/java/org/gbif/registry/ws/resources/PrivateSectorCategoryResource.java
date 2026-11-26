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

import org.gbif.registry.service.privatesector.PrivateSectorCategoryService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.security.UserRoles.ADMIN_ROLE;

/** Admin trigger for the dataset category sync from publishers' private sector machine tags. */
@Hidden
@Slf4j
@RestController
@RequestMapping(value = "dataset/privateSectorCategory")
public class PrivateSectorCategoryResource {

  private final PrivateSectorCategoryService privateSectorCategoryService;
  private final Executor asyncExecutor;

  public PrivateSectorCategoryResource(
      PrivateSectorCategoryService privateSectorCategoryService,
      @Autowired(required = false) @Qualifier("boundedTaskExecutor") Executor asyncExecutor) {
    this.privateSectorCategoryService = privateSectorCategoryService;
    this.asyncExecutor = asyncExecutor;
  }

  /**
   * Syncs all datasets of tagged publishers, and cleans up datasets that still have categories
   * from a publisher that lost its tag. Runs in the background; progress and the result are logged.
   */
  @PostMapping("sync")
  @Secured(ADMIN_ROLE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void syncAll() {
    Runnable sync =
        () -> {
          try {
            privateSectorCategoryService.syncAll();
          } catch (Exception e) {
            log.error("Private sector category sync of all datasets failed", e);
          }
        };
    if (asyncExecutor != null) {
      CompletableFuture.runAsync(sync, asyncExecutor);
    } else {
      CompletableFuture.runAsync(sync);
    }
  }
}
