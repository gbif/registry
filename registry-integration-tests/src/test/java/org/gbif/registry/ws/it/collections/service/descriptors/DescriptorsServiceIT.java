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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.descriptors.Descriptor;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.model.collections.request.DescriptorGroupSearchRequest;
import org.gbif.api.model.collections.request.DescriptorSearchRequest;
import org.gbif.api.model.common.export.ExportFormat;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.service.collections.CollectionService;
import org.gbif.api.service.collections.DescriptorsService;
import org.gbif.api.util.GrSciCollUtils;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.test.mocks.NameUsageMatchingServiceMock;
import org.gbif.registry.ws.it.collections.service.BaseServiceIT;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import lombok.SneakyThrows;

import static org.junit.jupiter.api.Assertions.*;

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
    long descriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            "My descriptor set",
            "description",
            Set.of("test-tag"),
            collection.getKey());
    assertTrue(descriptorGroupKey > 0);

    assertEquals(
        1,
        descriptorsService
            .listDescriptorGroups(
                collection.getKey(), DescriptorGroupSearchRequest.builder().build())
            .getResults()
            .size());

    PagingResponse<Descriptor> descriptors =
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(5, descriptors.getResults().size());
    assertTrue(descriptors.getResults().stream().allMatch(r -> r.getVerbatim().size() == 5));

    // check the order of the verbatim fields is the same as in the file
    descriptors
        .getResults()
        .forEach(
            d -> {
              Iterator<String> verbatimKeysIt = d.getVerbatim().keySet().iterator();
              assertEquals("dwc:scientificName", verbatimKeysIt.next());
              assertEquals("Num. of Specimens", verbatimKeysIt.next());
              assertEquals("Num. Databased", verbatimKeysIt.next());
              assertEquals("Num. Imaged", verbatimKeysIt.next());
            });

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
                        Collections.singletonList(NameUsageMatchingServiceMock.DEFAULT_USAGE.getKey()))
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
                            NameUsageMatchingServiceMock.DEFAULT_HIGHEST_USAGE.getKey()))
                    .build())
            .getResults()
            .size());

    Resource descriptorsFile2 = new ClassPathResource("collections/descriptors2.csv");
    descriptorsService.updateDescriptorGroup(
        descriptorGroupKey,
        StreamUtils.copyToByteArray(descriptorsFile2.getInputStream()),
        ExportFormat.TSV,
        "My descriptor set",
        Set.of("updated-tag"),
        "description");

    descriptors = descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(4, descriptors.getResults().size());
    assertTrue(descriptors.getResults().stream().allMatch(r -> r.getVerbatim().size() == 4));
    descriptors =
        descriptorsService.listDescriptors(
            DescriptorSearchRequest.builder().usageName(Collections.singletonList("Aves")).build());
    assertEquals(2, descriptors.getResults().size());

    // check the order of the verbatim fields is the same as in the file
    descriptors
        .getResults()
        .forEach(
            d -> {
              Iterator<String> verbatimKeysIt = d.getVerbatim().keySet().iterator();
              assertEquals("dwc:scientificName", verbatimKeysIt.next());
              assertEquals("Num. of Specimens", verbatimKeysIt.next());
              assertEquals("Num. Databased", verbatimKeysIt.next());
              assertEquals("dwc:country", verbatimKeysIt.next());
            });

    descriptorsService.deleteDescriptorGroup(descriptorGroupKey);
    assertEquals(
        0,
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build()).getCount());
    assertEquals(
        0,
        descriptorsService
            .listDescriptorGroups(
                collection.getKey(), DescriptorGroupSearchRequest.builder().build())
            .getCount());
  }

  @Test
  @SneakyThrows
  public void ihDescriptorsTest() {
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    collectionService.addMasterSourceMetadata(
        collection.getKey(), new MasterSourceMetadata(Source.IH_IRN, "dsfgds"));

    String title = "My descriptor set";
    String description = "description";

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    long descriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            title,
            description,
            Set.of("test-tag"),
            collection.getKey());
    assertTrue(descriptorGroupKey > 0);

    assertEquals(
        1,
        descriptorsService
            .listDescriptorGroups(
                collection.getKey(), DescriptorGroupSearchRequest.builder().build())
            .getResults()
            .size());

    PagingResponse<Descriptor> descriptors =
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(5, descriptors.getResults().size());

    collectionService.addMachineTag(
        collection.getKey(),
        new MachineTag(
            GrSciCollUtils.IH_NS,
            GrSciCollUtils.COLL_SUMMARY_MT,
            String.valueOf(descriptorGroupKey)));

    descriptorsService.updateDescriptorGroup(
        descriptorGroupKey,
        StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
        ExportFormat.TSV,
        "foo",
        Set.of("updated-tag"),
        "foo");

    DescriptorGroup updated = descriptorsService.getDescriptorGroup(descriptorGroupKey);
    assertEquals(title, updated.getTitle());
    assertEquals(description, updated.getDescription());

    descriptorsService.deleteDescriptorGroup(descriptorGroupKey);
    assertNull(descriptorsService.getDescriptorGroup(descriptorGroupKey).getDeleted());
  }

  @Test
  @SneakyThrows
  public void reinterpretationTest() {
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors2.csv");
    long descriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            "My descriptor set",
            "description",
            Set.of("test-tag"),
            collection.getKey());

    long descriptorsCount =
        descriptorsService.countDescriptors(
            DescriptorSearchRequest.builder().descriptorGroupKey(descriptorGroupKey).build());

    assertDoesNotThrow(() -> descriptorsService.reinterpretDescriptorGroup(descriptorGroupKey));

    PagingResponse<DescriptorGroup> descriptorGroups =
        descriptorsService.listDescriptorGroups(
            collection.getKey(), DescriptorGroupSearchRequest.builder().build());
    assertEquals(1, descriptorGroups.getCount());
    assertEquals(descriptorGroupKey, descriptorGroups.getResults().get(0).getKey());
    assertEquals(
        descriptorsCount,
        descriptorsService.countDescriptors(
            DescriptorSearchRequest.builder().descriptorGroupKey(descriptorGroupKey).build()));

    assertDoesNotThrow(
        () -> descriptorsService.reinterpretCollectionDescriptorGroups(collection.getKey()));
    assertDoesNotThrow(descriptorsService::reinterpretAllDescriptorGroups);

    descriptorGroups =
        descriptorsService.listDescriptorGroups(
            collection.getKey(), DescriptorGroupSearchRequest.builder().build());
    assertEquals(1, descriptorGroups.getCount());
    assertEquals(descriptorGroupKey, descriptorGroups.getResults().get(0).getKey());
    assertEquals(
        descriptorsCount,
        descriptorsService.countDescriptors(
            DescriptorSearchRequest.builder().descriptorGroupKey(descriptorGroupKey).build()));

    PagingResponse<Descriptor> descriptors =
      descriptorsService.listDescriptors(
        DescriptorSearchRequest.builder().usageName(Collections.singletonList("Aves")).build());
    assertEquals(2, descriptors.getResults().size());
  }

  @Test
  @SneakyThrows
  public void searchDescriptorGroupByTagsTest() {
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    Resource descriptorsFile = new ClassPathResource("collections/descriptors.csv");
    long descriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            "My descriptor set",
            "description",
            Set.of("test-tag"),
            collection.getKey());
    assertTrue(descriptorGroupKey > 0);

    // Search with matching tag
    PagingResponse<DescriptorGroup> response = descriptorsService.listDescriptorGroups(
        collection.getKey(),
        DescriptorGroupSearchRequest.builder()
            .tags(Set.of("test-tag"))
            .build());
    assertEquals(1, response.getCount());
    assertEquals("test-tag", response.getResults().get(0).getTags().iterator().next());

    // Search with non-matching tag
    response = descriptorsService.listDescriptorGroups(
        collection.getKey(),
        DescriptorGroupSearchRequest.builder()
            .tags(Set.of("non-matching-tag"))
            .build());
    assertEquals(0, response.getCount());

    // Search with multiple tags
    response = descriptorsService.listDescriptorGroups(
        collection.getKey(),
        DescriptorGroupSearchRequest.builder()
            .tags(Set.of("test-tag", "another-tag"))
            .build());
    assertEquals(1, response.getCount());

    // Update descriptor group with new tags
    descriptorsService.updateDescriptorGroup(
        descriptorGroupKey,
        StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
        ExportFormat.TSV,
        "My descriptor set",
        Set.of("updated-tag"),
        "description");

    // Search with updated tag
    response = descriptorsService.listDescriptorGroups(
        collection.getKey(),
        DescriptorGroupSearchRequest.builder()
            .tags(Set.of("updated-tag"))
            .build());
    assertEquals(1, response.getCount());
    assertEquals("updated-tag", response.getResults().get(0).getTags().iterator().next());
  }

  @Test
  @SneakyThrows
  public void vocabularyValidationTest() {
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    // Create a descriptor group with vocabulary fields
    Resource descriptorsFile = new ClassPathResource("collections/descriptors4.csv");
    long descriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            "Vocabulary Test Set",
            "Testing vocabulary validation",
            Set.of("vocab-test"),
            collection.getKey());
    assertTrue(descriptorGroupKey > 0);

    // Get descriptors and check vocabulary validation
    PagingResponse<Descriptor> descriptors =
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(3, descriptors.getResults().size());

    // Check that valid vocabulary values are preserved
    Descriptor validDescriptor = descriptors.getResults().stream()
        .filter(d -> "Test Species".equals(d.getVerbatim().get("dwc:scientificName")))
        .findFirst()
        .orElseThrow();

    // Valid biome should be preserved
    assertNotNull(validDescriptor.getBiome());
    assertEquals("Tropical", validDescriptor.getBiome());

    // Valid object classification should be preserved
    assertNotNull(validDescriptor.getObjectClassification());
    assertEquals("Specimen", validDescriptor.getObjectClassification());

    // Valid biome type should be preserved
    assertNotNull(validDescriptor.getBiomeType());
    assertEquals("Terrestrial", validDescriptor.getBiomeType());

    // Check test descriptor with mixed values
    Descriptor testDesc = descriptors.getResults().stream()
        .filter(d -> "Another Species".equals(d.getVerbatim().get("dwc:scientificName")))
        .findFirst()
        .orElseThrow();

    assertNotNull(testDesc.getBiome());
    assertEquals("Desert", testDesc.getBiome());

    assertNull(testDesc.getObjectClassification());
    assertNull(testDesc.getBiomeType());


    // Check that empty values are handled correctly
    Descriptor emptyDescriptor = descriptors.getResults().stream()
        .filter(d -> "Third Species".equals(d.getVerbatim().get("dwc:scientificName")))
        .findFirst()
        .orElseThrow();

    // Empty biome should remain null
    assertNull(emptyDescriptor.getBiome());

    // Valid object classification should be preserved
    assertNotNull(emptyDescriptor.getObjectClassification());
    assertEquals("ValidSpecimen", emptyDescriptor.getObjectClassification());

    // Clean up
    descriptorsService.deleteDescriptorGroup(descriptorGroupKey);
  }

  @Test
  @SneakyThrows
  public void vocabularyValidationWithReinterpretationTest() {
    Collection collection = new Collection();
    collection.setCode("c1");
    collection.setName("n1");
    collectionService.create(collection);

    // Create a descriptor group with vocabulary fields
    Resource descriptorsFile = new ClassPathResource("collections/descriptors4.csv");
    long descriptorGroupKey =
        descriptorsService.createDescriptorGroup(
            StreamUtils.copyToByteArray(descriptorsFile.getInputStream()),
            ExportFormat.TSV,
            "Vocabulary Reinterpretation Test",
            "Testing vocabulary validation with reinterpretation",
            Set.of("vocab-reinterpret-test"),
            collection.getKey());
    assertTrue(descriptorGroupKey > 0);

    // Reinterpret the descriptor group to trigger vocabulary validation
    assertDoesNotThrow(() -> descriptorsService.reinterpretDescriptorGroup(descriptorGroupKey));

    // Get descriptors and verify vocabulary validation still works after reinterpretation
    PagingResponse<Descriptor> descriptors =
        descriptorsService.listDescriptors(DescriptorSearchRequest.builder().build());
    assertEquals(3, descriptors.getResults().size());

    // Check that valid vocabulary values are still preserved after reinterpretation
    Descriptor validDescriptor = descriptors.getResults().stream()
        .filter(d -> "Test Species".equals(d.getVerbatim().get("dwc:scientificName")))
        .findFirst()
        .orElseThrow();

    assertNotNull(validDescriptor.getBiome());
    assertEquals("Tropical", validDescriptor.getBiome());
    assertNotNull(validDescriptor.getObjectClassification());
    assertEquals("Specimen", validDescriptor.getObjectClassification());
    assertNotNull(validDescriptor.getBiomeType());
    assertEquals("Terrestrial", validDescriptor.getBiomeType());

    // Check test descriptor after reinterpretation
    Descriptor testDesc = descriptors.getResults().stream()
        .filter(d -> "Another Species".equals(d.getVerbatim().get("dwc:scientificName")))
        .findFirst()
        .orElseThrow();

    // Biome: not vocabulary-managed, "Desert" preserved
    assertNotNull(testDesc.getBiome());
    assertEquals("Desert", testDesc.getBiome());

    // Object classification: vocabulary-managed, "foo" becomes null
    assertNull(testDesc.getObjectClassification());
    assertNull(testDesc.getBiomeType());

    // Check that empty values are handled correctly after reinterpretation
    Descriptor emptyDescriptor = descriptors.getResults().stream()
        .filter(d -> "Third Species".equals(d.getVerbatim().get("dwc:scientificName")))
        .findFirst()
        .orElseThrow();

    // Empty biome should remain null
    assertNull(emptyDescriptor.getBiome());

    // Valid object classification should be preserved
    assertNotNull(emptyDescriptor.getObjectClassification());
    assertEquals("ValidSpecimen", emptyDescriptor.getObjectClassification());

    assertNull(emptyDescriptor.getBiomeType());

    // Clean up
    descriptorsService.deleteDescriptorGroup(descriptorGroupKey);
  }
}
