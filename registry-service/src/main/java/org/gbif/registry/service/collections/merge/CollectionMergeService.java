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
package org.gbif.registry.service.collections.merge;

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Collection;
import org.gbif.api.service.collections.CollectionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

/** Service to merge duplicated {@link Collection}. */
@Service
public class CollectionMergeService extends BaseMergeService<Collection> {

  @Autowired
  protected CollectionMergeService(CollectionService collectionService) {
    super(collectionService);
  }

  @Override
  void checkMergeExtraPreconditions(Collection entityToReplace, Collection replacement) {
    if (entityToReplace.getInstitutionKey() != null
        && !entityToReplace.getInstitutionKey().equals(replacement.getInstitutionKey())) {
      throw new IllegalArgumentException(
          "Cannot do the replacement because the collections don't belong to the same institution");
    }
    Preconditions.checkArgument(
        entityToReplace.getReplacedBy() == null, "Cannot merge an entity that was replaced");
    Preconditions.checkArgument(
        replacement.getReplacedBy() == null, "Cannot do a merge with an entity that was replaced");
  }

  @Override
  Collection mergeEntityFields(Collection entityToReplace, Collection replacement) {
    setNullFieldsInTarget(replacement, entityToReplace);
    replacement.setEmail(mergeLists(entityToReplace.getEmail(), replacement.getEmail()));
    replacement.setPhone(mergeLists(entityToReplace.getPhone(), replacement.getPhone()));
    replacement.setContentTypes(
        mergeLists(entityToReplace.getContentTypes(), replacement.getContentTypes()));
    replacement.setPreservationTypes(
        mergeLists(entityToReplace.getPreservationTypes(), replacement.getPreservationTypes()));
    replacement.setIncorporatedCollections(
        mergeLists(
            entityToReplace.getIncorporatedCollections(),
            replacement.getIncorporatedCollections()));

    // codes of the replaced entity are added as alternative codes of the replacement
    replacement
        .getAlternativeCodes()
        .add(
            new AlternativeCode(
                entityToReplace.getCode(),
                "Code from replaced entity " + entityToReplace.getKey()));
    replacement.getAlternativeCodes().addAll(entityToReplace.getAlternativeCodes());

    return replacement;
  }

  @Override
  void additionalOperations(Collection entityToReplace, Collection replacement) {}
}
