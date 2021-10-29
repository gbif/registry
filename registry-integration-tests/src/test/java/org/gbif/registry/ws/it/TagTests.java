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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.service.registry.TagService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TagTests {

  public static <T extends NetworkEntity> void testTagErroneousDelete(
      TagService service, T entity) {
    Tag tag1 = new Tag();
    tag1.setCreatedBy(entity.getCreatedBy());
    tag1.setValue("tag1");
    int tagKey = service.addTag(entity.getKey(), tag1);
    service.deleteTag(UUID.randomUUID(), tagKey); // wrong parent UUID
    // nothing happens - expected?
  }

  public static <T extends NetworkEntity> void testAddDelete(TagService service, T entity) {
    List<Tag> tags = service.listTags(entity.getKey(), null);
    assertNotNull(tags, "Tag list should be empty, not null when no tags exist");
    assertTrue(tags.isEmpty(), "Tags should be empty when none added");
    Tag tag1 = new Tag();
    tag1.setCreatedBy(entity.getCreatedBy());
    tag1.setValue("tag1");

    Tag tag2 = new Tag();
    tag2.setCreatedBy(entity.getCreatedBy());
    tag2.setValue("tag2");

    service.addTag(entity.getKey(), tag1);
    service.addTag(entity.getKey(), tag2);
    tags = service.listTags(entity.getKey(), null);
    assertNotNull(tags);
    assertEquals(2, tags.size(), "2 tags have been added");
    service.deleteTag(entity.getKey(), tags.get(0).getKey());
    tags = service.listTags(entity.getKey(), null);
    assertNotNull(tags);
    assertEquals(1, tags.size(), "1 tag should remain after the deletion");
  }
}
