/*
 * Copyright 2013 Global Biodiversity Information Facility (GBIF)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.ws.resources;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * A resource that provides a JSON serialization of all Enumerations in the GBIF API suitable for building Javascript
 * based clients. This has no Java client, since Java clients have access to the Enums directly.
 * Reflection can be used to generate the inventory of enumerations.
 */
@Path("enumeration")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Singleton
public class EnumerationResource {

  private static Logger LOG = LoggerFactory.getLogger(EnumerationResource.class);

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

  /**
   * An inventory of the enumerations supported.
   *
   * @return The enumerations in the GBIF API.
   */
  @GET
  @Path("basic")
  public Set<String> inventory() {
    return PATH_MAPPING.keySet();
  }

  // reflect over the package to find suitable enumerations
  private static Map<String, Enum<?>[]> enumerations() {
    try {
      ClassPath cp = ClassPath.from(EnumerationResource.class.getClassLoader());
      ImmutableMap.Builder<String, Enum<?>[]> builder = ImmutableMap.builder();

      List<ClassInfo> infos = cp.getTopLevelClasses(Country.class.getPackage().getName()).asList();
      for (ClassInfo info : infos) {
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
  @Path("country")
  @GET
  public List<Map<String, String>> listCountries() {
    return COUNTRIES;
  }

  /**
   * @return list of language information based on our enum.
   */
  @Path("language")
  @GET
  public List<Map<String, String>> listLanguages() {
    return LANGUAGES;
  }

  /**
   * @return list of 'deserialised' License enums: uses License URL or just the enum name if no URL exists
   */
  @Path("license")
  @GET
  public List<String> listLicenses() {
    return LICENSES;
  }

  /**
   * Gets the values of the named enumeration should the enumeration exist.
   * Note this is used by the AngularJS console.
   *
   * @param name Which should be the enumeration name in the GBIF vocabulary package (e.g. Country)
   * @return The enumeration values or null if the enumeration does not exist.
   */
  @Path("basic/{name}")
  @GET()
  @NullToNotFound
  public Enum<?>[] getEnumeration(@PathParam("name") @NotNull String name) {
    if (PATH_MAPPING.containsKey(name)) {
      return PATH_MAPPING.get(name);
    } else {
      return null;
    }
  }

  /**
   * Transform a {@link Country} into a key-value map of properties.
   *
   * @param country
   *
   * @return
   */
  private static Map<String, String> countryToMap(Country country) {
    Map<String, String> info = Maps.newHashMap();
    info.put("iso2", country.getIso2LetterCode());
    info.put("iso3", country.getIso3LetterCode());
    info.put("isoNumerical", String.valueOf(country.getIsoNumericalCode()));
    info.put("title", country.getTitle());
    Optional.ofNullable(country.getGbifRegion()).ifPresent( gbifRegion -> info.put("gbifRegion", country.getGbifRegion().name()));
    info.put("enumName", country.name());
    return info;
  }

  /**
   * Transform a {@link Language} into a key-value map of properties.
   *
   * @param language
   *
   * @return
   */
  private static Map<String, String> languageToMap(Language language) {
    Map<String, String> info = Maps.newHashMap();
    info.put("iso2", language.getIso2LetterCode());
    info.put("iso3", language.getIso3LetterCode());
    info.put("title", language.getTitleEnglish());
    info.put("titleNative", language.getTitleNative());
    return info;
  }


}
