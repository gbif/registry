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

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.vocabulary.collections.MasterSourceType;
import org.gbif.api.vocabulary.collections.Source;
import org.gbif.registry.database.TestCaseDatabaseInitializer;
import org.gbif.registry.persistence.mapper.MachineTagMapper;
import org.gbif.registry.persistence.mapper.collections.CollectionMapper;
import org.gbif.registry.persistence.mapper.collections.DuplicatesMapper;
import org.gbif.registry.persistence.mapper.collections.InstitutionMapper;
import org.gbif.registry.persistence.mapper.collections.MasterSourceSyncMetadataMapper;
import org.gbif.registry.persistence.mapper.collections.dto.DuplicateMetadataDto;
import org.gbif.registry.search.test.EsManageServer;
import org.gbif.registry.ws.it.BaseItTest;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import static org.gbif.registry.domain.collections.Constants.IDIGBIO_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DuplicatesMapperIT extends BaseItTest {

  @RegisterExtension
  protected TestCaseDatabaseInitializer databaseRule =
      new TestCaseDatabaseInitializer(database.getPostgresContainer(), "collection", "institution");

  private DuplicatesMapper duplicatesMapper;
  private InstitutionMapper institutionMapper;
  private CollectionMapper collectionMapper;
  private MachineTagMapper machineTagMapper;
  private MasterSourceSyncMetadataMapper masterSourceMetadataMapper;

  @Autowired
  public DuplicatesMapperIT(
      DuplicatesMapper duplicatesMapper,
      InstitutionMapper institutionMapper,
      CollectionMapper collectionMapper,
      MachineTagMapper machineTagMapper,
      MasterSourceSyncMetadataMapper masterSourceMetadataMapper,
      SimplePrincipalProvider principalProvider,
      EsManageServer esServer) {
    super(principalProvider, esServer);
    this.duplicatesMapper = duplicatesMapper;
    this.institutionMapper = institutionMapper;
    this.collectionMapper = collectionMapper;
    this.machineTagMapper = machineTagMapper;
    this.masterSourceMetadataMapper = masterSourceMetadataMapper;
  }

  @Test
  public void institutionMetadataTest() {
    Institution inst1 = new Institution();
    inst1.setKey(UUID.randomUUID());
    inst1.setActive(true);
    inst1.setCode("i1");
    inst1.setName("n1");
    inst1.setCreatedBy("test");
    inst1.setModifiedBy("test");
    institutionMapper.create(inst1);

    MachineTag mt = new MachineTag(IDIGBIO_NAMESPACE, "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    institutionMapper.addMachineTag(inst1.getKey(), mt.getKey());

    MasterSourceMetadata masterSourceMetadata = new MasterSourceMetadata(Source.IH_IRN, "foo");
    masterSourceMetadata.setCreatedBy("test");
    masterSourceMetadataMapper.create(masterSourceMetadata);
    institutionMapper.addMasterSourceMetadata(
        inst1.getKey(), masterSourceMetadata.getKey(), MasterSourceType.IH);

    List<DuplicateMetadataDto> metadataDtos =
        duplicatesMapper.getInstitutionsMetadata(Collections.singleton(inst1.getKey()));
    assertEquals(1, metadataDtos.size());
    assertTrue(metadataDtos.get(0).isActive());
    assertTrue(metadataDtos.get(0).isIdigbio());
    assertTrue(metadataDtos.get(0).isIh());

    Institution inst2 = new Institution();
    inst2.setKey(UUID.randomUUID());
    inst2.setCode("i2");
    inst2.setName("n1");
    inst2.setCreatedBy("test");
    inst2.setModifiedBy("test");

    institutionMapper.create(inst2);
    metadataDtos = duplicatesMapper.getInstitutionsMetadata(Collections.singleton(inst2.getKey()));
    assertEquals(1, metadataDtos.size());
    assertFalse(metadataDtos.get(0).isActive());
    assertFalse(metadataDtos.get(0).isIdigbio());
    assertFalse(metadataDtos.get(0).isIh());

    metadataDtos =
        duplicatesMapper.getInstitutionsMetadata(
            new HashSet<>(Arrays.asList(inst1.getKey(), inst2.getKey())));
    assertEquals(2, metadataDtos.size());
  }

  @Test
  public void collectionMetadataTest() {
    Collection c1 = new Collection();
    c1.setKey(UUID.randomUUID());
    c1.setActive(true);
    c1.setCode("c1");
    c1.setName("n1");
    c1.setCreatedBy("test");
    c1.setModifiedBy("test");
    collectionMapper.create(c1);

    MachineTag mt = new MachineTag(IDIGBIO_NAMESPACE, "test", "foo");
    mt.setCreatedBy("test");
    machineTagMapper.createMachineTag(mt);
    collectionMapper.addMachineTag(c1.getKey(), mt.getKey());

    MasterSourceMetadata masterSourceMetadata = new MasterSourceMetadata(Source.IH_IRN, "foo");
    masterSourceMetadata.setCreatedBy("test");
    masterSourceMetadataMapper.create(masterSourceMetadata);
    collectionMapper.addMasterSourceMetadata(
        c1.getKey(), masterSourceMetadata.getKey(), MasterSourceType.IH);

    List<DuplicateMetadataDto> metadataDtos =
        duplicatesMapper.getCollectionsMetadata(Collections.singleton(c1.getKey()));
    assertEquals(1, metadataDtos.size());
    assertTrue(metadataDtos.get(0).isActive());
    assertTrue(metadataDtos.get(0).isIdigbio());
    assertTrue(metadataDtos.get(0).isIh());

    Collection c2 = new Collection();
    c2.setKey(UUID.randomUUID());
    c2.setCode("c2");
    c2.setName("n1");
    c2.setCreatedBy("test");
    c2.setModifiedBy("test");
    collectionMapper.create(c2);

    metadataDtos = duplicatesMapper.getCollectionsMetadata(Collections.singleton(c2.getKey()));
    assertEquals(1, metadataDtos.size());
    assertFalse(metadataDtos.get(0).isActive());
    assertFalse(metadataDtos.get(0).isIdigbio());
    assertFalse(metadataDtos.get(0).isIh());

    metadataDtos =
        duplicatesMapper.getCollectionsMetadata(
            new HashSet<>(Arrays.asList(c1.getKey(), c2.getKey())));
    assertEquals(2, metadataDtos.size());
  }
}
