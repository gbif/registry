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
package org.gbif.registry.ws.resources.collections;

import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.search.collections.KeyCodeNameResult;
import org.gbif.api.service.collections.InstitutionService;
import org.gbif.registry.events.EventManager;
import org.gbif.registry.persistence.WithMyBatis;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.TagMapper;
import org.gbif.registry.persistence.mapper.collections.AddressMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.ws.security.EditorAuthorizationService;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

import static org.gbif.registry.ws.util.GrscicollUtils.GRSCICOLL_PATH;

/**
 * Class that acts both as the WS endpoint for {@link Institution} entities and also provides an *
 * implementation of {@link InstitutionService}.
 */
@RestController
@RequestMapping(
    value = GRSCICOLL_PATH + "/institution",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class InstitutionEntityResource extends ExtendedCollectionEntityResource<Institution>
    implements InstitutionService {

  private final InstitutionMapper institutionMapper;

  public InstitutionEntityResource(
      InstitutionMapper institutionMapper,
      AddressMapper addressMapper,
      IdentifierMapper identifierMapper,
      TagMapper tagMapper,
      MachineTagMapper machineTagMapper,
      EditorAuthorizationService userAuthService,
      EventManager eventManager,
      WithMyBatis withMyBatis) {
    super(
        institutionMapper,
        addressMapper,
        tagMapper,
        identifierMapper,
        institutionMapper,
        machineTagMapper,
        eventManager,
        Institution.class,
        userAuthService,
        withMyBatis);
    this.institutionMapper = institutionMapper;
  }

  @GetMapping
  @Override
  public PagingResponse<Institution> list(
      @Nullable @RequestParam(value = "q", required = false) String query,
      @Nullable @RequestParam(value = "contact", required = false) UUID contactKey,
      @Nullable @RequestParam(value = "code", required = false) String code,
      @Nullable @RequestParam(value = "name", required = false) String name,
      Pageable page) {
    page = page == null ? new PagingRequest() : page;
    query = query != null ? Strings.emptyToNull(CharMatcher.whitespace().trimFrom(query)) : query;
    long total = institutionMapper.count(query, contactKey, code, name);
    return new PagingResponse<>(
        page, total, institutionMapper.list(query, contactKey, code, name, page));
  }

  @GetMapping("deleted")
  @Override
  public PagingResponse<Institution> listDeleted(Pageable page) {
    page = page == null ? new PagingRequest() : page;
    return new PagingResponse<>(
        page, institutionMapper.countDeleted(), institutionMapper.deleted(page));
  }

  @GetMapping("suggest")
  @Override
  public List<KeyCodeNameResult> suggest(@RequestParam(value = "q", required = false) String q) {
    return institutionMapper.suggest(q);
  }
}
