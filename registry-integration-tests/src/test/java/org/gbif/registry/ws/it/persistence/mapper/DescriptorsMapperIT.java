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
package org.gbif.registry.ws.it.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.descriptors.DescriptorGroup;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.DescriptorsMapper;
import org.gbif.registry.persistence.mapper.collections.dto.DescriptorDto;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorGroupParams;
import org.gbif.registry.persistence.mapper.collections.params.DescriptorParams;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

public class DescriptorsMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(
          "collection",
          "collection_descriptor_group",
          "collection_descriptor",
          "collection_descriptor_verbatim");

  private final DescriptorsMapper descriptorsMapper;
  private final CollectionMapper collectionMapper;

  @Autowired
  public DescriptorsMapperIT(
      DescriptorsMapper descriptorsMapper,
      CollectionMapper collectionMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.descriptorsMapper = descriptorsMapper;
    this.collectionMapper = collectionMapper;
  }

  @Test
  public void crudTest() {
    Collection collection = new Collection();
    collection.setCode("code");
    collection.setName("name");
    collection.setKey(UUID.randomUUID());
    collection.setCreatedBy("test");
    collection.setModifiedBy("test");
    collectionMapper.create(collection);
    assertNotNull(collection.getKey());

    DescriptorGroup descriptorGroup = new DescriptorGroup();
    descriptorGroup.setTitle("title");
    descriptorGroup.setDescription("description");
    descriptorGroup.setCreatedBy("user");
    descriptorGroup.setModifiedBy("user");
    descriptorGroup.setCollectionKey(collection.getKey());
    descriptorsMapper.createDescriptorGroup(descriptorGroup);
    assertTrue(descriptorGroup.getKey() > 0);

    DescriptorGroup created = descriptorsMapper.getDescriptorGroup(descriptorGroup.getKey());
    assertTrue(descriptorGroup.lenientEquals(created));

    created.setTitle("title2");
    descriptorsMapper.updateDescriptorGroup(created);

    DescriptorGroup updated = descriptorsMapper.getDescriptorGroup(descriptorGroup.getKey());
    assertTrue(updated.lenientEquals(created));

    assertEquals(
        1,
        descriptorsMapper
            .listDescriptorGroups(
                DescriptorGroupParams.builder().collectionKey(collection.getKey()).build())
            .size());

    DescriptorDto descriptorDto = new DescriptorDto();
    descriptorDto.setDescriptorGroupKey(descriptorGroup.getKey());
    descriptorDto.setCountry(Country.DENMARK);
    descriptorDto.setDiscipline("discipline");
    descriptorDto.setIssues(Arrays.asList("i1", "i2"));
    descriptorDto.setTypeStatus(Collections.singletonList(TypeStatus.ALLOLECTOTYPE.name()));
    descriptorDto.setUsageRank(Rank.ABERRATION);
    descriptorDto.setUsageName("usage");
    descriptorDto.setUsageKey(5);

    descriptorDto.setTaxonClassification(
        Arrays.asList(
            new RankedName(1, "Kingdom", Rank.KINGDOM), new RankedName(3, "Phylum", Rank.PHYLUM)));
    descriptorsMapper.createDescriptor(descriptorDto);
    assertTrue(descriptorDto.getKey() > 0);

    descriptorsMapper.createVerbatim(descriptorDto.getKey(), "f1", "v1");
    descriptorsMapper.createVerbatim(descriptorDto.getKey(), "f2", "v2");

    DescriptorDto createdDescriptor = descriptorsMapper.getDescriptor(descriptorDto.getKey());
    assertEquals(2, createdDescriptor.getIssues().size());
    assertEquals(1, createdDescriptor.getTypeStatus().size());
    assertEquals(Country.DENMARK, createdDescriptor.getCountry());
    assertEquals(2, createdDescriptor.getVerbatim().size());
    assertEquals(Rank.ABERRATION, createdDescriptor.getUsageRank());
    assertEquals(2, createdDescriptor.getTaxonClassification().size());

    assertEquals(2, descriptorsMapper.getVerbatimNames(descriptorGroup.getKey()).size());

    assertEquals(
        1,
        descriptorsMapper
            .listDescriptors(
                DescriptorParams.builder().descriptorGroupKey(descriptorGroup.getKey()).build())
            .size());

    assertEquals(
        1,
        descriptorsMapper
            .listDescriptors(
                DescriptorParams.builder().usageKey(Collections.singletonList(5)).build())
            .size());

    assertEquals(
        1,
        descriptorsMapper
            .listDescriptors(
                DescriptorParams.builder()
                    .usageRank(Collections.singletonList(Rank.ABERRATION))
                    .build())
            .size());

    descriptorsMapper.deleteDescriptorGroup(descriptorGroup.getKey());
    assertTrue(
        descriptorsMapper
            .listDescriptorGroups(
                DescriptorGroupParams.builder().collectionKey(collection.getKey()).build())
            .isEmpty());

    assertEquals(
        1,
        descriptorsMapper
            .listDescriptorGroups(DescriptorGroupParams.builder().deleted(true).build())
            .size());

    // update descriptor
    createdDescriptor.setCountry(Country.COCOS_ISLANDS);
    createdDescriptor.setUsageName("NEW");
    createdDescriptor.setTaxonClassification(
        Arrays.asList(new RankedName(1, "Kingdom2", Rank.KINGDOM)));
    descriptorsMapper.updateDescriptor(createdDescriptor);

    DescriptorDto updatedDescriptor = descriptorsMapper.getDescriptor(createdDescriptor.getKey());
    assertEquals(Country.COCOS_ISLANDS, updatedDescriptor.getCountry());
    assertEquals("NEW", updatedDescriptor.getUsageName());
    assertEquals(1, updatedDescriptor.getTaxonClassification().size());
    assertEquals("Kingdom2", updatedDescriptor.getTaxonClassification().get(0).getName());

    descriptorsMapper.deleteDescriptors(descriptorGroup.getKey());

    assertEquals(
        0,
        descriptorsMapper
            .listDescriptors(
                DescriptorParams.builder().descriptorGroupKey(descriptorGroup.getKey()).build())
            .size());
  }
}
