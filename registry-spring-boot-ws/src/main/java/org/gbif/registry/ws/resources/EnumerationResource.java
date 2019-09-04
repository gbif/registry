package org.gbif.registry.ws.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Extension;
import org.gbif.api.vocabulary.InterpretationRemark;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NameUsageIssue;
import org.gbif.api.vocabulary.OccurrenceIssue;
import org.gbif.api.vocabulary.collections.PreservationType;
import org.gbif.ws.annotation.NullToNotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * A resource that provides a JSON serialization of all Enumerations in the GBIF API suitable for building Javascript
 * based clients. This has no Java client, since Java clients have access to the Enums directly.
 * Reflection can be used to generate the inventory of enumerations.
 */
@RestController
@RequestMapping(value = "enumeration", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnumerationResource {

  private static final Logger LOG = LoggerFactory.getLogger(EnumerationResource.class);

  // Uses reflection to find the enumerations in the API
  private static final Map<String, Enum<?>[]> PATH_MAPPING = enumerations();

  //List of Licenses as String
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

  //Only includes InterpretationRemark that are NOT deprecated
  private static final List<Map<String, Object>> INTERPRETATION_REMARKS =
      Stream.concat(
          Arrays.stream(OccurrenceIssue.values()),
          Arrays.stream(NameUsageIssue.values()))
          .filter(val -> !val.isDeprecated())
          .map(val -> interpretationRemarkToMap(val)) //::interpretationRemarkToMap throws LambdaConversionException
          .collect(collectingAndThen(toList(), Collections::unmodifiableList));

  // Exists to avoid use of the ExtensionSerializer, which would try (but fail) to give row types as URLs.
  private static final List<String> BASIC_EXTENSIONS =
      Arrays.stream(Extension.values())
          .map(Extension::name)
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
    try {
      ClassPath cp = ClassPath.from(EnumerationResource.class.getClassLoader());
      ImmutableSortedMap.Builder<String, Enum<?>[]> builder = ImmutableSortedMap.naturalOrder();

      // create a list with gbif and collection vocabulary enums
      ImmutableList.Builder<ClassInfo> infosListBuilder = ImmutableList.<ClassInfo>builder()
          .addAll(cp.getTopLevelClasses(Country.class.getPackage().getName()).asList())
          .addAll(cp.getTopLevelClasses(PreservationType.class.getPackage().getName()).asList());

      for (ClassInfo info : infosListBuilder.build()) {
        Class<? extends Enum<?>> vocab = VocabularyUtils.lookupVocabulary(info.getName());
        // verify that it is an Enumeration
        if (vocab != null && vocab.getEnumConstants() != null) {
          builder.put(info.getSimpleName(), vocab.getEnumConstants());
        }
      }
      return builder.build();

    } catch (Exception e) {
      LOG.error("Unable to read the classpath for enumerations", e);
      return ImmutableMap.<String, Enum<?>[]>of(); // empty
    }
  }

  /**
   * @return list of country information based on our enum.
   */
  @GetMapping("country")
  public List<Map<String, String>> listCountries() {
    return COUNTRIES;
  }

  /**
   * @return list of language information based on our enum.
   */
  @GetMapping("language")
  public List<Map<String, String>> listLanguages() {
    return LANGUAGES;
  }

  /**
   * @return list of 'deserialised' License enums: uses License URL or just the enum name if no URL exists
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
   * Gets the Extension enumeration.  This exists to avoid use of the ExtensionSerializer, which assumes JSON keys,
   * since in this case we want values.
   *
   * @return The enumeration values.
   */
  @GetMapping("basic/Extension")
  public List<String> getExtensionEnumeration() {
    return BASIC_EXTENSIONS;
  }

  /**
   * Gets the values of the named enumeration should the enumeration exist.
   * Note this is used by the AngularJS console.
   *
   * @param name Which should be the enumeration name in the GBIF vocabulary package (e.g. Country)
   * @return The enumeration values or null if the enumeration does not exist.
   */
  @GetMapping("basic/{name}")
  @NullToNotFound
  public Enum<?>[] getEnumeration(@PathVariable("name") @NotNull String name) {
    return PATH_MAPPING.getOrDefault(name, null);
  }

  /**
   * Transform a {@link Country} into a key-value map of properties.
   */
  private static Map<String, String> countryToMap(Country country) {
    Map<String, String> info = new LinkedHashMap<>();
    info.put("iso2", country.getIso2LetterCode());
    info.put("iso3", country.getIso3LetterCode());
    info.put("isoNumerical", String.valueOf(country.getIsoNumericalCode()));
    info.put("title", country.getTitle());
    Optional.ofNullable(country.getGbifRegion()).ifPresent(gbifRegion -> info.put("gbifRegion", country.getGbifRegion().name()));
    info.put("enumName", country.name());
    return info;
  }

  /**
   * Transform a {@link Language} into a key-value map of properties.
   */
  private static Map<String, String> languageToMap(Language language) {
    Map<String, String> info = new LinkedHashMap<>();
    info.put("iso2", language.getIso2LetterCode());
    info.put("iso3", language.getIso3LetterCode());
    info.put("title", language.getTitleEnglish());
    info.put("titleNative", language.getTitleNative());
    return info;
  }

  /**
   * Transform a {@link InterpretationRemark} into a key-value map of properties.
   */
  private static Map<String, Object> interpretationRemarkToMap(InterpretationRemark interpretationRemark) {
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", interpretationRemark.getId());
    info.put("severity", interpretationRemark.getSeverity().name());
    info.put("relatedTerms", interpretationRemark.getRelatedTerms());
    return info;
  }
}
