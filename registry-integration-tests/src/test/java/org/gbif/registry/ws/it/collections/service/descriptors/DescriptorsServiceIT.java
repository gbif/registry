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
package org.gbif.registry.ws.it.collections.service.descriptors;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import lombok.SneakyThrows;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.test.mocks.NubResourceClientMock;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/** Tests the {@link CollectionService}. */
public class DescriptorsServiceIT extends BaseServiceIT {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule = new TestCaseDatabaseInitializer();

  private final DescriptorsService descriptorsService;
  private final CollectionService collectionService;

  @Autowired
  public DescriptorsServiceIT(
      DescriptorsService descriptorsService,
      CollectionService collectionService,
      SimplePrincipalProvider principalProvider) {
    super(principalProvider);
    this.descriptorsService = descriptorsService;
    this.collectionService = collectionService;
  }

  @Test
  @SneakyThrows
  public void descriptorsTest() {
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    long DescriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            "My descriptor set",
            "description",
            collection.getKey());
    assertTrue(DescriptorGroupKey > 0);

    assertEquals(
        1,
        descriptorsService
            .listDescriptorGroups(collection.getKey(), DescriptorGroupSearchRequest.builder().build())
            .getResults()
            .size());

    PagingResponse<Descriptor> descriptors =
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(5, descriptors.getResults().size());
    assertTrue(descriptors.getResults().stream().allMatch(r -> r.getVerbatim().size() == 4));

    assertEquals(
        0,
        descriptorsService
            .listDescriptors(
                DescriptorSearchRequest.builder()
                    .usageName(Collections.singletonList("foo"))
                    .build())
            .getResults()
            .size());

    assertEquals(
        5,
        descriptorsService
            .listDescriptors(
                DescriptorSearchRequest.builder()
                    .usageKey(
                        Collections.singletonList(NubResourceClientMock.DEFAULT_USAGE.getKey()))
                    .build())
            .getResults()
            .size());

    assertEquals(
        5,
        descriptorsService
            .listDescriptors(
                DescriptorSearchRequest.builder()
                    .taxonKey(
                        Collections.singletonList(
                            NubResourceClientMock.DEFAULT_HIGHEST_USAGE.getKey()))
                    .build())
            .getResults()
            .size());

    Resource descriptorsFile2 = new ClassPathResource("collections/descriptors2.csv");
    descriptorsService.updateDescriptorGroup(
        DescriptorGroupKey,
        StreamUtils.copyToByteArray(descriptorsFile2.getInputStream()),
        ExportFormat.TSV,
        "My descriptor set",
        "description");

    descriptors = descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(4, descriptors.getResults().size());
    assertTrue(descriptors.getResults().stream().allMatch(r -> r.getVerbatim().size() == 3));

    descriptorsService.deleteDescriptorGroup(DescriptorGroupKey);
    assertEquals(
        0,
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build()).getCount());
    assertEquals(
        0,
        descriptorsService
            .listDescriptorGroups(collection.getKey(), DescriptorGroupSearchRequest.builder().build())
            .getCount());
  }
}
