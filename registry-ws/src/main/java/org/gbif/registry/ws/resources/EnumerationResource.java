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
package org.gbif.registry.ws.resources;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.literature.LiteratureType;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.*;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.dwc.terms.Term;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;

import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * A resource that provides a JSON serialization of all Enumerations in the GBIF API suitable for
 * building Javascript based clients. This has no Java client, since Java clients have access to the
 * Enums directly. Reflection can be used to generate the inventory of enumerations.
 */
@Validated
@RestController
@RequestMapping(value = "enumeration", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnumerationResource {

  private static final Logger LOG = LoggerFactory.getLogger(EnumerationResource.class);

  // Uses reflection to find the enumerations in the API
  private static final Map<String, Enum<?>[]> PATH_MAPPING = enumerations();
  private static final Map<String, String> EXTENSIONS_BY_NAME_MAPPING =
      Arrays.stream(Extension.values())
          .collect(Collectors.toMap(Extension::name, Extension::getRowType));
  private static final Map<String, String> EXTENSIONS_BY_ROWTYPE_MAPPING =
      Arrays.stream(Extension.values())
          .collect(Collectors.toMap(Extension::getRowType, Extension::name));

  // List of Licenses as String
  private static final List<String> LICENSES =
      Arrays.stream(License.values())
          .map(license -> license.isConcrete() ? license.getLicenseUrl() : license.name())
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  private static final List<Map<String, String>> COUNTRIES =
      Arrays.stream(Country.values())
          .filter(Country::isOfficial)
          .map(EnumerationResource::countryToMap)
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  private static final List<Map<String, String>> LANGUAGES =
      Arrays.stream(Language.values())
          .map(EnumerationResource::languageToMap)
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  // Only includes InterpretationRemark that are NOT deprecated
  @SuppressWarnings("Convert2MethodRef")
  private static final List<Map<String, Object>> INTERPRETATION_REMARKS =
      Stream.concat(Arrays.stream(OccurrenceIssue.values()), Arrays.stream(NameUsageIssue.values()))
          .filter(val -> !val.isDeprecated())
          .map(
              val ->
                  interpretationRemarkToMap(
                      val)) // ::interpretationRemarkToMap throws LambdaConversionException
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  // Exists to avoid use of the ExtensionSerializer, which would try (but fail) to give row types as
  // URLs.
  private static final List<String> BASIC_EXTENSIONS =
      Arrays.stream(Extension.values())
          .map(Extension::name)
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  private static final List<String> EXTENSION_ROW_TYPES =
      Arrays.stream(Extension.values())
          .map(Extension::getRowType)
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  /**
   * An inventory of the enumerations supported.
   *
   * @return The enumerations in the GBIF API.
   */
  @GetMapping("basic")
  public Set<String> inventory() {
    return PATH_MAPPING.keySet();
  }

  // reflect over the package to find suitable enumerations
  private static Map<String, Enum<?>[]> enumerations() {
    ImmutableSortedMap.Builder<String, Enum<?>[]> builder = ImmutableSortedMap.naturalOrder();
    ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
    MetadataReaderFactory metadataReaderFactory =
        new CachingMetadataReaderFactory(resourcePatternResolver);

    List<Class<? extends Serializable>> classes =
        Arrays.asList(
            Country.class, PreservationType.class, PipelineStep.class, LiteratureType.class);

    ImmutableSortedMap<String, Enum<?>[]> result;
    try {
      for (Class<? extends Serializable> clazz : classes) {
        addEnumResources(
            builder,
            metadataReaderFactory,
            resourcePatternResolver.getResources(
                ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                    + resolveBasePackage(clazz.getPackage().getName())
                    + "/*.class"));
      }
      result = builder.build();
    } catch (Exception e) {
      LOG.error("Unable to read the classpath for enumerations", e);
      result = ImmutableSortedMap.of(); // empty
    }

    return result;
  }

  private static void addEnumResources(
      Builder<String, Enum<?>[]> builder,
      MetadataReaderFactory metadataReaderFactory,
      Resource[] resources)
      throws IOException {
    for (Resource resource : resources) {
      if (resource.isReadable()) {
        MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
        String className = metadataReader.getClassMetadata().getClassName();
        Class<? extends Enum<?>> vocab = VocabularyUtils.lookupVocabulary(className);
        if (isEnumeration(metadataReader)) {
          builder.put(
              org.apache.commons.lang.ClassUtils.getShortClassName(className),
              vocab.getEnumConstants());
        }
      }
    }
  }

  private static String resolveBasePackage(String basePackage) {
    return ClassUtils.convertClassNameToResourcePath(
        SystemPropertyUtils.resolvePlaceholders(basePackage));
  }

  private static boolean isEnumeration(MetadataReader metadataReader) {
    Class<? extends Enum<?>> vocab =
        VocabularyUtils.lookupVocabulary(metadataReader.getClassMetadata().getClassName());

    return vocab != null && vocab.getEnumConstants() != null;
  }

  /** @return list of country information based on our enum. */
  @GetMapping("country")
  public List<Map<String, String>> listCountries() {
    return COUNTRIES;
  }

  /** @return list of language information based on our enum. */
  @GetMapping("language")
  public List<Map<String, String>> listLanguages() {
    return LANGUAGES;
  }

  /**
   * @return list of 'deserialized' License enums: uses License URL or just the enum name if no URL
   *     exists
   */
  @GetMapping("license")
  public List<String> listLicenses() {
    return LICENSES;
  }

  @GetMapping("interpretationRemark")
  public List<Map<String, Object>> listInterpretationRemark() {
    return INTERPRETATION_REMARKS;
  }

  /**
   * Gets the Extension enumeration. This exists to avoid use of the ExtensionSerializer, which
   * assumes JSON keys, since in this case we want values.
   *
   * @return The enumeration values.
   */
  @GetMapping("basic/Extension")
  public List<String> getExtensionEnumeration() {
    return BASIC_EXTENSIONS;
  }

  @GetMapping("basic/Extension/{extensionName}")
  public String getExtension(@PathVariable("extensionName") String extensionName) {
    return EXTENSIONS_BY_NAME_MAPPING.getOrDefault(extensionName, null);
  }

  /**
   * Returns the keys of the Extension enum. It's done manually because the default for the
   * Extension enum is done in {@link #getExtensionEnumeration()} and cannot be changed to keep
   * backwards compatibility.
   */
  @GetMapping("ExtensionRowType")
  public ResponseEntity getExtensionRowTypesEnumeration(
      @RequestParam(value = "rowType", required = false) String rowType) {
    if (!Strings.isNullOrEmpty(rowType)) {
      return ResponseEntity.ok(EXTENSIONS_BY_ROWTYPE_MAPPING.getOrDefault(rowType, null));
    }
    return ResponseEntity.ok(EXTENSION_ROW_TYPES);
  }

  /**
   * Gets the values of the named enumeration should the enumeration exist. Note this is used by the
   * AngularJS console.
   *
   * @param name Which should be the enumeration name in the GBIF vocabulary package (e.g. Country)
   * @return The enumeration values or null if the enumeration does not exist.
   */
  @GetMapping("basic/{name}")
  @NullToNotFound("/enumeration/basic/{name}")
  public Object getEnumeration(@PathVariable("name") @NotNull String name) {
    return PATH_MAPPING.getOrDefault(name, null);
  }

  /** Transform a {@link Country} into a key-value map of properties. */
  private static Map<String, String> countryToMap(Country country) {
    Map<String, String> info = new LinkedHashMap<>();
    info.put("iso2", country.getIso2LetterCode());
    info.put("iso3", country.getIso3LetterCode());
    info.put("isoNumerical", String.valueOf(country.getIsoNumericalCode()));
    info.put("title", country.getTitle());
    Optional.ofNullable(country.getGbifRegion())
        .ifPresent(gbifRegion -> info.put("gbifRegion", country.getGbifRegion().name()));
    info.put("enumName", country.name());
    return info;
  }

  /** Transform a {@link Language} into a key-value map of properties. */
  private static Map<String, String> languageToMap(Language language) {
    Map<String, String> info = new LinkedHashMap<>();
    info.put("iso2", language.getIso2LetterCode());
    info.put("iso3", language.getIso3LetterCode());
    info.put("title", language.getTitleEnglish());
    info.put("titleNative", language.getTitleNative());
    return info;
  }

  /** Transform a {@link InterpretationRemark} into a key-value map of properties. */
  private static Map<String, Object> interpretationRemarkToMap(
      InterpretationRemark interpretationRemark) {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", interpretationRemark.getId());
    info.put("severity", interpretationRemark.getSeverity().name());
    info.put(
        "relatedTerms",
        interpretationRemark.getRelatedTerms().stream()
            .map(Term::qualifiedName)
            .collect(Collectors.toSet()));
    return info;
  }
}
