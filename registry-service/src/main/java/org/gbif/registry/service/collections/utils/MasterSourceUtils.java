package org.gbif.registry.service.collections.utils;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.MasterSourceType;
import org.gbif.api.model.collections.PrimaryCollectionEntity;
import org.gbif.api.model.collections.Sourceable;
import org.gbif.api.model.registry.MachineTaggable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MasterSourceUtils {

  // TODO: Modify IH Sync!!
  public static final String MASTER_SOURCE_COLLECTIONS_NAMESPACE =
      "master-source.collections.gbif.org";
  public static final String IH_SOURCE = "ih_irn";
  public static final String DATASET_SOURCE = "dataset";
  public static final String ORGANIZATION_SOURCE = "organization";

  public static final String CONTACTS_FIELD_NAME = "contactPersons";

  public static final Map<MasterSourceType, List<LockableField>> INSTITUTION_LOCKABLE_FIELDS =
      new EnumMap<>(MasterSourceType.class);

  public static final Map<MasterSourceType, List<LockableField>> COLLECTION_LOCKABLE_FIELDS =
      new EnumMap<>(MasterSourceType.class);

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
  }

  public static <T extends PrimaryCollectionEntity> boolean hasExternalMasterSource(T entity) {
    return entity != null && entity.getMasterSource() != MasterSourceType.GRSCICOLL;
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

    Optional.ofNullable(f.getDeclaredAnnotation(Sourceable.class))
        .map(Sourceable::masterSources)
        .ifPresent(
            sources ->
                Arrays.stream(sources)
                    .forEach(
                        ms ->
                            fieldsMap
                                .computeIfAbsent(ms, v -> new ArrayList<>())
                                .add(new LockableField(getter, setter))));
  }

  @AllArgsConstructor
  @Data
  public static class LockableField {
    Method getter;
    Method setter;
  }
}
