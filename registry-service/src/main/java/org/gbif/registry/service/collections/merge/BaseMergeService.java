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
package org.gbif.registry.service.collections.merge;

import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.persistence.ContactableMapper;
import org.gbif.registry.persistence.mapper.IdentifierMapper;
import org.gbif.registry.persistence.mapper.collections.BaseMapper;
import org.gbif.registry.persistence.mapper.collections.MergeableMapper;
import org.gbif.registry.persistence.mapper.collections.OccurrenceMappeableMapper;
import org.gbif.registry.persistence.mapper.collections.PersonMapper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class BaseMergeService<
        T extends
            CollectionEntity & Identifiable & MachineTaggable & OccurrenceMappeable & Contactable
                & Taggable & Commentable>
    implements MergeService {

  private final BaseMapper<T> baseMapper;
  private final MergeableMapper mergeableMapper;
  private final ContactableMapper contactableMapper;
  private final IdentifierMapper identifierMapper;
  private final OccurrenceMappeableMapper occurrenceMappeableMapper;
  protected final PersonMapper personMapper;

  protected BaseMergeService(
      BaseMapper<T> baseMapper,
      MergeableMapper mergeableMapper,
      ContactableMapper contactableMapper,
      IdentifierMapper identifierMapper,
      OccurrenceMappeableMapper occurrenceMappeableMapper,
      PersonMapper personMapper) {
    this.baseMapper = baseMapper;
    this.mergeableMapper = mergeableMapper;
    this.contactableMapper = contactableMapper;
    this.identifierMapper = identifierMapper;
    this.occurrenceMappeableMapper = occurrenceMappeableMapper;
    this.personMapper = personMapper;
  }

  @Override
  @Transactional
  public void merge(UUID entityToReplaceKey, UUID replacementKey, String user) {
    checkArgument(user != null, "User is required");

    T entityToReplace = baseMapper.get(entityToReplaceKey);
    checkArgument(
        entityToReplace != null, "Not found entity to replace with key " + entityToReplaceKey);

    T replacement = baseMapper.get(replacementKey);
    checkArgument(replacement != null, "Not found replacement entity with key " + replacementKey);

    // check IH_IRN identifiers. If both entities have them we don't allow to do the replacement
    // because we wouldn't know how to sync them with IH: if we move it to the replacement this
    // entity will be synced with 2 IH entities and the second sync will overwrite the first one; if
    // we don't move it, then the next IH sync will create a new entity for that IRN, hence the
    // replacement would be useless.
    if (containsIHIdentifier(entityToReplace) && containsIHIdentifier(replacement)) {
      throw new IllegalArgumentException(
          "Cannot do the replacement because both entities have an IH IRN identifier");
    }

    checkExtraPreconditions(entityToReplace, replacement);

    // delete and set the replacement
    mergeableMapper.replace(entityToReplaceKey, replacementKey);

    // copy the identifiers
    entityToReplace
        .getIdentifiers()
        .forEach(
            i -> mergeableMapper.moveIdentifier(entityToReplaceKey, replacementKey, i.getKey()));

    // copy iDigBio machine tags
    entityToReplace.getMachineTags().stream()
        .filter(mt -> mt.getNamespace().equals("iDigBio.org"))
        .forEach(
            mt -> mergeableMapper.moveMachineTag(entityToReplaceKey, replacementKey, mt.getKey()));

    // merge contacts
    Objects.requireNonNull(entityToReplace.getContacts()).stream()
        .filter(c -> !replacement.getContacts().contains(c))
        .forEach(c -> contactableMapper.addContact(replacementKey, c.getKey()));

    // add the UUID key of the replaced entity as an identifier of the replacement
    Identifier keyIdentifier =
        new Identifier(IdentifierType.UUID, entityToReplace.getKey().toString());
    keyIdentifier.setCreatedBy(user);
    identifierMapper.createIdentifier(keyIdentifier);
    baseMapper.addIdentifier(replacementKey, keyIdentifier.getKey());

    // merge entity fields
    baseMapper.update(mergeEntityFields(entityToReplace, replacement));

    // update occurrence mappings
    List<OccurrenceMapping> occMappings =
        occurrenceMappeableMapper.listOccurrenceMappings(entityToReplaceKey);
    occMappings.forEach(
        om ->
            mergeableMapper.moveOccurrenceMapping(entityToReplaceKey, replacementKey, om.getKey()));

    additionalOperations(entityToReplace, replacement);
  }

  protected boolean containsIHIdentifier(T entity) {
    return entity.getIdentifiers().stream().anyMatch(i -> i.getType() == IdentifierType.IH_IRN);
  }

  protected void setNullFields(T target, T source) {
    Class<T> clazz = (Class<T>) target.getClass();
    Arrays.stream(clazz.getDeclaredFields())
        .filter(f -> !f.getType().isAssignableFrom(List.class))
        .filter(
            f ->
                !f.getName().equals("created")
                    && !f.getName().equals("createdBy")
                    && !f.getName().equals("modified")
                    && !f.getName().equals("modifiedBy")
                    && !f.getName().equals("deleted")
                    && !f.getName().equals("replacedBy"))
        .forEach(
            f -> {
              try {
                UnaryOperator<String> capitalize =
                    s -> s.substring(0, 1).toUpperCase() + s.substring(1);

                // get value from target
                Object targetValue =
                    clazz.getMethod("get" + capitalize.apply(f.getName())).invoke(target);

                // if the field is null in target we use the value from source
                if (targetValue == null) {
                  // get value from source
                  Object sourceValue =
                      clazz.getMethod("get" + capitalize.apply(f.getName())).invoke(source);

                  // set value in target
                  if (sourceValue != null) {
                    clazz
                        .getMethod("set" + capitalize.apply(f.getName()), f.getType())
                        .invoke(target, sourceValue);
                  }
                }
              } catch (IllegalAccessException
                  | NoSuchMethodException
                  | InvocationTargetException e) {
                // ignore field
              }
            });
  }

  protected <R> List<R> mergeLists(List<R> l1, List<R> l2) {
    if (l1.isEmpty()) {
      return l2;
    }
    if (l2.isEmpty()) {
      return l1;
    }

    Set<R> uniqueValues = new HashSet<>(l1);
    uniqueValues.addAll(l2);
    return new ArrayList<>(uniqueValues);
  }

  abstract void checkExtraPreconditions(T entityToReplace, T replacement);

  abstract T mergeEntityFields(T entityToReplace, T replacement);

  abstract void additionalOperations(T entityToReplace, T replacement);
}
