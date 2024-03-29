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
package org.gbif.registry.service.collections.utils;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Sourceable;
import org.gbif.api.model.collections.SourceableField;
import org.gbif.api.model.collections.Sourceables;
import org.gbif.api.vocabulary.collections.MasterSourceType;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MasterSourceUtils {

  public static final String CONTACTS_FIELD_NAME = "contactPersons";
  public static final String IH_SYNC_USER = "ih-sync";

  public static final Map<MasterSourceType, List<LockableField>> INSTITUTION_LOCKABLE_FIELDS =
      new EnumMap<>(MasterSourceType.class);

  public static final Map<MasterSourceType, List<LockableField>> COLLECTION_LOCKABLE_FIELDS =
      new EnumMap<>(MasterSourceType.class);

  public static final List<SourceableField> INSTITUTION_SOURCEABLE_FIELDS = new ArrayList<>();
  public static final List<SourceableField> COLLECTION_SOURCEABLE_FIELDS = new ArrayList<>();

  static {
    // create lockable fields for Institution
    Arrays.stream(Institution.class.getDeclaredFields())
        .filter(
            f ->
                Arrays.stream(f.getDeclaredAnnotationsByType(Sourceable.class))
                    .anyMatch(a -> !a.overridable() && a.sourceableParts().length == 0))
        .forEach(f -> createLockableField(f, Institution.class, INSTITUTION_LOCKABLE_FIELDS));
    // create lockable fields for Collection
    Arrays.stream(Collection.class.getDeclaredFields())
        .filter(
            f ->
                Arrays.stream(f.getDeclaredAnnotationsByType(Sourceable.class))
                    .anyMatch(a -> !a.overridable() && a.sourceableParts().length == 0))
        .forEach(f -> createLockableField(f, Collection.class, COLLECTION_LOCKABLE_FIELDS));

    // create sourceable fields
    Arrays.stream(Institution.class.getDeclaredFields())
        .forEach(f -> createSourceableField(f).ifPresent(INSTITUTION_SOURCEABLE_FIELDS::add));
    Arrays.stream(Collection.class.getDeclaredFields())
        .forEach(f -> createSourceableField(f).ifPresent(COLLECTION_SOURCEABLE_FIELDS::add));
  }

  public static <T extends CollectionEntity> boolean hasExternalMasterSource(T entity) {
    return entity != null
        && entity.getMasterSource() != null
        && entity.getMasterSource() != MasterSourceType.GRSCICOLL;
  }

  public static <T extends CollectionEntity> boolean isLockableEntity(T entity) {
    boolean hasExternalMasterSource = hasExternalMasterSource(entity);

    if (!hasExternalMasterSource) {
      return false;
    }

    // if it's IH and is using the sync user it's allowed to modify the entiy
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    final String username = authentication.getName();
    return entity.getMasterSource() != MasterSourceType.IH || !username.equals(IH_SYNC_USER);
  }

  @SneakyThrows
  public static boolean isSourceableField(Class<?> clazz, String fieldName) {
    return clazz.getDeclaredField(fieldName).getDeclaredAnnotation(Sourceable.class) != null;
  }

  @SneakyThrows
  private static void createLockableField(
      Field f, Class<?> clazz, Map<MasterSourceType, List<LockableField>> fieldsMap) {
    String getterPrefix = f.getType().isAssignableFrom(Boolean.TYPE) ? "is" : "get";

    String methodName = f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1);

    Method getter = clazz.getDeclaredMethod(getterPrefix + methodName);
    Method setter = clazz.getDeclaredMethod("set" + methodName, f.getType());

    Consumer<Sourceable> processAnnotation =
        annotation -> {
          if (!annotation.overridable() && annotation.sourceableParts().length == 0) {
            Arrays.stream(annotation.masterSources())
                .forEach(
                    ms ->
                        fieldsMap
                            .computeIfAbsent(ms, v -> new ArrayList<>())
                            .add(new LockableField(getter, setter)));
          }
        };

    Optional.ofNullable(f.getDeclaredAnnotation(Sourceable.class)).ifPresent(processAnnotation);
    Optional.ofNullable(f.getDeclaredAnnotation(Sourceables.class))
        .map(Sourceables::value)
        .ifPresent(annotations -> Arrays.stream(annotations).forEach(processAnnotation));
  }

  private static Optional<SourceableField> createSourceableField(Field f) {
    Optional<Sourceable> sourceable =
        Optional.ofNullable(f.getDeclaredAnnotation(Sourceable.class));
    Optional<Sourceables> sourceables =
        Optional.ofNullable(f.getDeclaredAnnotation(Sourceables.class));

    if (sourceable.isPresent() || sourceables.isPresent()) {
      SourceableField sourceableField = new SourceableField();
      sourceableField.setFieldName(f.getName());

      List<Sourceable> sourceablesList = new ArrayList<>();
      sourceable.ifPresent(sourceablesList::add);
      sourceables.ifPresent(value -> Collections.addAll(sourceablesList, value.value()));

      // there shouldn't be more than annotation with the same master source
      sourceablesList.forEach(
          s ->
              Arrays.stream(s.masterSources())
                  .forEach(
                      ms -> {
                        SourceableField.Source source = new SourceableField.Source();
                        source.setMasterSource(ms);
                        source.setOverridable(s.overridable());
                        source.setSourceableParts(Arrays.asList(s.sourceableParts()));
                        sourceableField.getSources().add(source);
                      }));

      return Optional.of(sourceableField);
    }
    return Optional.empty();
  }

  @AllArgsConstructor
  @Data
  public static class LockableField {
    Method getter;
    Method setter;
  }
}
