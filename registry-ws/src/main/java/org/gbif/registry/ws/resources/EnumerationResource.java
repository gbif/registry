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

import io.swagger.v3.oas.annotations.Hidden;

import org.gbif.api.annotation.NullToNotFound;
import org.gbif.api.model.literature.LiteratureType;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.InterpretationRemark;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.OccurrenceIssue;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.dwc.terms.Term;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.http.MediaType;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedMap.Builder;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * A resource that provides a JSON serialization of all Enumerations in the GBIF API suitable for
 * building Javascript based clients. This has no Java client, since Java clients have access to the
 * Enums directly. Reflection can be used to generate the inventory of enumerations.
 */
@OpenAPIDefinition(
  info = @Info(
    title = "Registry API",
    version = "v1",
    description =
        "This API works against the GBIF Registry, which makes all registered Datasets, Installations, Organizations, " +
          "Nodes, and Networks discoverable.\n\n" +
        "Internally we use a Java web service client for the consumption of these HTTP-based, RESTful web services. " +
          "It may be of interest to those coding against the API, and can be found in the " +
          "[registry-ws-client](https://github.com/gbif/registry/tree/master/registry-ws-client) project.\n\n" +
        "Please note the old Registry API is still supported, but is now deprecated. Anyone starting new work is " +
          "strongly encouraged to use the new API.",
    termsOfService = "https://www.gbif.org/terms"),
  servers = {
    @Server(url = "https://api.gbif.org/v1/", description = "Production"),
    @Server(url = "https://api.gbif-uat.org/v1/", description = "User testing")
  })
@Tag(name = "Enumerations", description = "This API provides JSON serializations of all enumerations in the GBIF API.",
  extensions = @io.swagger.v3.oas.annotations.extensions.Extension(
    name = "Order", properties = @ExtensionProperty(name = "Order", value = "5000")))
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

  private static final List<Map<String, Object>> EXTENSIONS =
      Arrays.stream(Extension.values())
        .map(EnumerationResource::extensionToMap)
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
  @Operation(
    operationId = "enumerationsBasic",
    summary = "An inventory of all enumerations")
  @ApiResponse(
    responseCode = "200",
    description = "List of enumerations.")
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
  @Operation(
    operationId = "enumerationCountry",
    summary = "Show the Country enumeration",
    description = "Lists the known countries, territories and islands based on ISO 3166-2")
  @ApiResponse(
    responseCode = "200",
    description = "Country, territory and island list.")
  @GetMapping("country")
  public List<Map<String, String>> listCountries() {
    return COUNTRIES;
  }

  /** @return list of language information based on our enum. */
  @Operation(
    operationId = "enumerationLanguage",
    summary = "Show the Language enumeration",
    description = "Lists the known languages based on ISO 639-1")
  @ApiResponse(
    responseCode = "200",
    description = "Language list.")
  @GetMapping("language")
  public List<Map<String, String>> listLanguages() {
    return LANGUAGES;
  }

  /**
   * @return list of 'deserialized' License enums: uses License URL or just the enum name if no URL
   *     exists
   */
  @Operation(
    operationId = "enumerationLicense",
    summary = "Show the License enumeration",
    description = "Lists the accepted licenses")
  @ApiResponse(
    responseCode = "200",
    description = "License list.")
  @GetMapping("license")
  public List<String> listLicenses() {
    return LICENSES;
  }

  @Operation(
    operationId = "enumerationInterpretationRemark",
    summary = "Show the Interpretation Remark enumeration",
    description = "Lists the known interpretation remarks")
  @ApiResponse(
    responseCode = "200",
    description = "Interpretation remark list.")
  @GetMapping("interpretationRemark")
  public List<Map<String, Object>> listInterpretationRemark() {
    return INTERPRETATION_REMARKS;
  }

  /** @return list of DWCA extensions based on our enum. */
  @Operation(
    operationId = "enumerationExtension",
    summary = "Show the Extension enumeration",
    description = "Lists the known Darwin Core Archive extensions and their RowType")
  @ApiResponse(
    responseCode = "200",
    description = "Extension list.")
  @GetMapping("extension")
  public List<Map<String, Object>> listExtensions() {
    return EXTENSIONS;
  }

  /**
   * Gets the Extension enumeration. This exists to avoid use of the ExtensionSerializer, which
   * assumes JSON keys, since in this case we want values.
   *
   * @return The enumeration values.
   */
  @Hidden // Covered by documentation for getEnumeration
  @GetMapping("basic/Extension")
  public List<String> getExtensionEnumeration() {
    return BASIC_EXTENSIONS;
  }

  /**
   * Gets the values of the named enumeration should the enumeration exist. Note this is used by the
   * AngularJS console.
   *
   * @param name The enumeration name in the GBIF vocabulary package (e.g. Country)
   * @return The enumeration values or null if the enumeration does not exist.
   */
  @Operation(
    operationId = "enumerationBasic",
    summary = "Show a summary of an enumeration",
    description = "Lists the values of the given enumeration")
  @Parameter(
    name = "name",
    description = "The name of the enumeration",
    schema = @Schema(implementation = String.class),
    in = ParameterIn.PATH
  )
  @ApiResponses(
    value = {
      @ApiResponse(
        responseCode = "200",
        description = "Country, territory and island list."),
      @ApiResponse(
        responseCode = "404",
        description = "Unknown enumeration.",
        content = @Content),
    })
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

  /** Transform an {@link Extension} into a key-value map of properties. */
  private static Map<String, Object> extensionToMap(Extension extension) {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", extension.name());
    info.put("rowType", extension.getRowType());
    return info;
  }
}
