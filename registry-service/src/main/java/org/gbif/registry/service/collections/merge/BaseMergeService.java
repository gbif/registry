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

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Contactable;
import org.gbif.api.model.collections.MasterSourceMetadata;
import org.gbif.api.model.collections.OccurrenceMappeable;
import org.gbif.api.model.collections.OccurrenceMapping;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.registry.Commentable;
import org.gbif.api.model.registry.Identifiable;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.MachineTaggable;
import org.gbif.api.model.registry.Taggable;
import org.gbif.api.service.collections.PrimaryCollectionEntityService;
import org.gbif.api.vocabulary.IdentifierType;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.gbif.registry.domain.collections.Constants.IDIGBIO_NAMESPACE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_ADMIN_ROLE;
import static org.gbif.registry.security.UserRoles.GRSCICOLL_MEDIATOR_ROLE;
import static org.gbif.registry.service.collections.utils.MasterSourceUtils.hasExternalMasterSource;

public abstract class BaseMergeService<
        T extends
            PrimaryCollectionEntity & Identifiable & MachineTaggable & OccurrenceMappeable
                & Contactable & Taggable & Commentable>
    implements MergeService<T> {

  protected final PrimaryCollectionEntityService<T> primaryEntityService;

  protected BaseMergeService(PrimaryCollectionEntityService<T> primaryEntityService) {
    this.primaryEntityService = primaryEntityService;
  }

  @Secured({GRSCICOLL_ADMIN_ROLE, GRSCICOLL_MEDIATOR_ROLE})
  @Transactional
  @Override
  public void merge(UUID entityToReplaceKey, UUID replacementKey) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    checkArgument(!Strings.isNullOrEmpty(authentication.getName()));
    checkArgument(
        !entityToReplaceKey.equals(replacementKey),
        "The replacement has to be different than the entity to replace");

    T entityToReplace = primaryEntityService.get(entityToReplaceKey);
    checkArgument(
        entityToReplace != null, "Not found entity to replace with key " + entityToReplaceKey);
    checkArgument(entityToReplace.getDeleted() == null, "Cannot merge a deleted entity");

    T replacement = primaryEntityService.get(replacementKey);
    checkArgument(replacement != null, "Not found replacement entity with key " + replacementKey);
    checkArgument(
        replacement.getDeleted() == null, "Cannot merge an entity with a deleted replacement");

    // we don't allow to merge 2 entities that have external master source.
    // E.g.: for IH, if both entities have them we don't allow to do the replacement
    // because we wouldn't know how to sync them with IH: if we move it to the replacement this
    // entity will be synced with 2 IH entities and the second sync will overwrite the first one; if
    // we don't move it, then the next IH sync will create a new entity for that IRN, hence the
    // replacement would be useless.
    if (hasExternalMasterSource(entityToReplace) && hasExternalMasterSource(replacement)) {
      throw new IllegalArgumentException(
          "Cannot do the replacement because both entities have an external master source");
    }

    if (isIDigBioRecord(entityToReplace) && isIDigBioRecord(replacement)) {
      throw new IllegalArgumentException(
          "Cannot do the replacement because both entities are iDigBio records");
    }

    checkMergeExtraPreconditions(entityToReplace, replacement);

    // delete and set the replacement
    primaryEntityService.replace(entityToReplaceKey, replacementKey);

    // merge entity fields
    T updatedEntityToReplace = mergeEntityFields(entityToReplace, replacement);
    primaryEntityService.update(updatedEntityToReplace);

    // copy the identifiers
    entityToReplace.getIdentifiers().stream()
        .filter(i -> !containsIdentifier(replacement, i))
        .forEach(
            i ->
                primaryEntityService.addIdentifier(
                    replacementKey, new Identifier(i.getType(), i.getIdentifier())));

    // copy iDigBio and IH machine tags
    entityToReplace.getMachineTags().stream()
        .filter(mt -> mt.getNamespace().equals(IDIGBIO_NAMESPACE))
        .filter(mt -> !containsMachineTag(replacement, mt))
        .forEach(
            mt ->
                primaryEntityService.addMachineTag(
                    replacementKey,
                    new MachineTag(mt.getNamespace(), mt.getName(), mt.getValue())));

    // copy master source
    if (entityToReplace.getMasterSourceMetadata() != null) {
      primaryEntityService.addMasterSourceMetadata(
          replacementKey,
          new MasterSourceMetadata(
              entityToReplace.getMasterSourceMetadata().getSource(),
              entityToReplace.getMasterSourceMetadata().getSourceId()));
    }

    // FIXME: to be removed in the future, contacts are deprecated
    // merge contacts
    entityToReplace.getContacts().stream()
        .filter(c -> !replacement.getContacts().contains(c))
        .forEach(c -> primaryEntityService.addContact(replacementKey, c.getKey()));

    // merge contact persons
    entityToReplace.getContactPersons().stream()
        .filter(c -> replacement.getContactPersons().stream().noneMatch(cp -> cp.lenientEquals(c)))
        .forEach(
            c -> {
              c.setKey(null);
              primaryEntityService.addContactPerson(replacementKey, c);
            });

    // add the UUID key of the replaced entity as an identifier of the replacement
    Identifier keyIdentifier =
        new Identifier(IdentifierType.UUID, entityToReplace.getKey().toString());
    primaryEntityService.addIdentifier(replacementKey, keyIdentifier);

    // update occurrence mappings
    List<OccurrenceMapping> occMappings =
        primaryEntityService.listOccurrenceMappings(entityToReplaceKey);
    occMappings.stream()
        .filter(om -> !containsOccurrenceMapping(replacement, om))
        .forEach(
            om -> {
              om.setKey(null);
              primaryEntityService.addOccurrenceMapping(
                  replacementKey,
                  new OccurrenceMapping(om.getCode(), om.getIdentifier(), om.getDatasetKey()));
            });

    additionalOperations(entityToReplace, replacement);
  }

  protected boolean isIDigBioRecord(T entity) {
    return entity.getMachineTags().stream()
        .anyMatch(mt -> mt.getNamespace().equals(IDIGBIO_NAMESPACE));
  }

  protected boolean containsIdentifier(T entity, Identifier identifier) {
    return entity.getIdentifiers().stream().anyMatch(i -> i.lenientEquals(identifier));
  }

  protected boolean containsMachineTag(T entity, MachineTag machineTag) {
    return entity.getMachineTags().stream().anyMatch(mt -> mt.lenientEquals(machineTag));
  }

  protected boolean containsOccurrenceMapping(T entity, OccurrenceMapping occurrenceMapping) {
    return entity.getOccurrenceMappings().stream()
        .anyMatch(om -> om.lenientEquals(occurrenceMapping));
  }

  /** Sets the fields that are null in target with the value of the source. */
  protected void setNullFieldsInTarget(T target, T source) {
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
                    if (sourceValue instanceof Address) {
                      // set the key to null to create the address
                      ((Address) sourceValue).setKey(null);
                    }

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

  abstract void checkMergeExtraPreconditions(T entityToReplace, T replacement);

  abstract T mergeEntityFields(T entityToReplace, T replacement);

  abstract void additionalOperations(T entityToReplace, T replacement);
}
