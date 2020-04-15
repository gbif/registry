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
package org.gbif.registry.ws.it;

import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.CommentService;
import org.gbif.registry.test.TestDataFactory;

import java.util.List;

import static org.gbif.registry.ws.it.LenientAssert.assertLenientEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CommentTests {

  public static <T extends NetworkEntity> void testAddDelete(
      CommentService service, T entity, TestDataFactory testDataFactory) {

    // check there are none on a newly created entity
    List<Comment> comments = service.listComments(entity.getKey());
    assertNotNull("Comment list should be empty, not null when no comments exist", comments);
    assertTrue("Comment should be empty when none added", comments.isEmpty());

    // test additions
    service.addComment(entity.getKey(), testDataFactory.newComment());
    service.addComment(entity.getKey(), testDataFactory.newComment());
    comments = service.listComments(entity.getKey());
    assertNotNull(comments);
    assertEquals("2 comments have been added", 2, comments.size());

    // test deletion, ensuring correct one is deleted
    service.deleteComment(entity.getKey(), comments.get(0).getKey());
    comments = service.listComments(entity.getKey());
    assertNotNull(comments);
    assertEquals("1 comment should remain after the deletion", 1, comments.size());
    Comment expected = testDataFactory.newComment();
    Comment created = comments.get(0);
    assertLenientEquals("Created entity does not read as expected", expected, created);
  }
}
