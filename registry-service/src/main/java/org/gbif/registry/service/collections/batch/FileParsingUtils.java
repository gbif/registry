package org.gbif.registry.service.collections.batch;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.UserId;
import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.collections.IdType;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

public class FileParsingUtils {

  static final String LIST_DELIMITER = "\\|";
  static final String FIELD_DELIMITER = ":";

  static ParserResult<Address> parseAddress(
      String address, String city, String province, String postalCode, String country) {
    if (Strings.isNullOrEmpty(address)
        && Strings.isNullOrEmpty(city)
        && Strings.isNullOrEmpty(province)
        && Strings.isNullOrEmpty(postalCode)
        && Strings.isNullOrEmpty(country)) {
      return ParserResult.empty();
    }

    Address addressObject = new Address();
    addressObject.setAddress(address);
    addressObject.setCity(city);
    addressObject.setProvince(province);
    addressObject.setPostalCode(postalCode);

    ParserResult<Country> parsedCountry = parseCountry(country);
    parsedCountry.getResult().ifPresent(addressObject::setCountry);

    return ParserResult.of(addressObject, parsedCountry.getErrors());
  }

  static ParserResult<Country> parseCountry(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return ParserResult.empty();
    }
    try {
      Country country = Country.fromIsoCode(value);
      if (country == null) {
        country = Country.valueOf(value);
      }
      return ParserResult.of(country);
    } catch (Exception ex) {
      ParserResult.fail("Incorrect country: " + value);
    }

    return ParserResult.empty();
  }

  static <T> ParserResult<T> parseSingleValue(String value, Function<String, T> mapper) {
    if (Strings.isNullOrEmpty(value)) {
      return ParserResult.empty();
    }
    return ParserResult.of(mapper.apply(value));
  }

  static <T extends Enum<T>> ParserResult<T> parseEnum(String value, Function<String, T> mapper) {
    try {
      return parseSingleValue(value, mapper);
    } catch (Exception ex) {
      return ParserResult.fail("Wrong enum value: " + value);
    }
  }

  static ParserResult<URI> parseUri(String value) {
    try {
      return parseSingleValue(value, URI::create);
    } catch (Exception ex) {
      return ParserResult.fail("Wrong uri: " + value);
    }
  }

  static ParserResult<BigDecimal> parseBigDecimal(String value) {
    try {
      return parseSingleValue(value, BigDecimal::new);
    } catch (Exception ex) {
      return ParserResult.fail("Wrong number: " + value);
    }
  }

  static ParserResult<Integer> parseInteger(String value) {
    try {
      return parseSingleValue(value, Integer::valueOf);
    } catch (Exception ex) {
      return ParserResult.fail("Wrong number: " + value);
    }
  }

  static ParserResult<Boolean> parseBoolean(String value) {
    try {
      return parseSingleValue(value, Boolean::valueOf);
    } catch (Exception ex) {
      return ParserResult.fail("Wrong boolean: " + value);
    }
  }

  static <T> ParserResult<List<T>> parseListValues(
      String list, Function<String, ParserResult<T>> mapper) {
    if (Strings.isNullOrEmpty(list)) {
      return ParserResult.empty();
    }

    String[] values = list.split(LIST_DELIMITER);
    if (values.length == 0) {
      return ParserResult.empty();
    }

    List<T> result = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (String v : values) {
      ParserResult<T> parserResult = mapper.apply(v);
      parserResult.getResult().ifPresent(result::add);
      Optional.ofNullable(parserResult.getErrors()).ifPresent(errors::addAll);
    }

    return ParserResult.of(result, errors);
  }

  static ParserResult<List<AlternativeCode>> parseAlternativeCodes(String alternativeCodes) {
    if (Strings.isNullOrEmpty(alternativeCodes)) {
      return ParserResult.empty();
    }

    String[] altCodes = alternativeCodes.split(LIST_DELIMITER);
    if (altCodes.length == 0) {
      return ParserResult.empty();
    }

    List<AlternativeCode> alternativeCodesList = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (String altCode : altCodes) {
      String[] values = altCode.split(FIELD_DELIMITER);
      if (values.length == 2) {
        alternativeCodesList.add(new AlternativeCode(values[0], values[1]));
      } else {
        errors.add("Incorrect alternative code: " + altCode);
      }
    }

    return ParserResult.of(alternativeCodesList, errors);
  }

  static ParserResult<List<Identifier>> parseIdentifiers(String identifiers) {
    if (Strings.isNullOrEmpty(identifiers)) {
      return ParserResult.empty();
    }

    String[] ids = identifiers.split(LIST_DELIMITER);
    if (ids.length == 0) {
      return ParserResult.empty();
    }

    List<Identifier> idsList = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (String id : ids) {
      ParserResult<Identifier> parserResult = parseIdentifier(id);
      parserResult.getResult().ifPresent(idsList::add);
      Optional.ofNullable(parserResult.getErrors()).ifPresent(errors::addAll);
    }

    return ParserResult.of(idsList, errors);
  }

  static ParserResult<Identifier> parseIdentifier(String identifier) {
    if (Strings.isNullOrEmpty(identifier)) {
      return ParserResult.empty();
    }

    String[] values = identifier.split(FIELD_DELIMITER);
    if (values.length != 2) {
      return ParserResult.fail("Incorrect identifier: " + identifier);
    }

    return ParserResult.of(new Identifier(IdentifierType.fromString(values[0]), values[1]));
  }

  static Optional<List<String>> parseStringList(String list) {
    return Optional.ofNullable(list)
        .filter(s -> !s.isEmpty())
        .map(s -> Arrays.asList(s.split(LIST_DELIMITER)));
  }

  static ParserResult<UUID> parseUUID(String value) {
    try {
      return parseSingleValue(value, UUID::fromString);
    } catch (Exception ex) {
      return ParserResult.fail("Incorrect UUID: " + value);
    }
  }

  static ParserResult<List<UserId>> parseUserIds(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return ParserResult.empty();
    }

    String[] ids = value.split(LIST_DELIMITER);
    if (ids.length == 0) {
      return ParserResult.empty();
    }

    List<UserId> userIdsList = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    for (String id : ids) {
      String[] values = id.split(FIELD_DELIMITER);
      if (values.length == 2) {
        ParserResult<IdType> parserResult = parseEnum(values[0], IdType::valueOf);
        parserResult.getResult().ifPresent(type -> userIdsList.add(new UserId(type, values[1])));
        Optional.ofNullable(parserResult.getErrors()).ifPresent(errors::addAll);
      } else {
        errors.add("Incorrect user ID: " + id);
      }
    }

    return ParserResult.of(userIdsList, errors);
  }

  static ParserResult<DOI> parseDoi(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return ParserResult.empty();
    }

    try {
      return ParserResult.of(new DOI(value));
    } catch (Exception ex) {
      return ParserResult.fail("Failed to parse DOI: " + value);
    }
  }

  static ParserResult<Map<String, Integer>> parseCollectionsSummary(String value) {
    if (Strings.isNullOrEmpty(value)) {
      return ParserResult.empty();
    }

    String[] values = value.split(LIST_DELIMITER);
    if (values.length == 0) {
      return ParserResult.empty();
    }

    Map<String, Integer> result = new HashMap<>();
    List<String> errors = new ArrayList<>();
    for (String v : values) {
      String[] fields = v.split(FIELD_DELIMITER);
      if (fields.length != 2) {
        errors.add("Invalid format of collection summary: " + v);
        continue;
      }
      try {
        result.put(fields[0], Integer.valueOf(fields[1]));
      } catch (Exception ex) {
        errors.add("Invalid count in collection summary: " + fields[1]);
      }
    }

    return ParserResult.of(result, errors);
  }

  /** Makes sure there is an element for each header. */
  public static String[] normalizeValues(int headersSize, String[] readValues) {
    // fill empty columns
    String[] values = Arrays.copyOf(readValues, headersSize);
    for (int i = readValues.length; i < values.length; i++) {
      values[i] = "";
    }
    return values;
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  static class ParserResult<T> {
    private T result;
    private List<String> errors;

    static <T> ParserResult<T> empty() {
      return new ParserResult<>(null, null);
    }

    static <T> ParserResult<T> of(T result) {
      return new ParserResult<>(result, null);
    }

    static <T> ParserResult<T> of(T result, List<String> errors) {
      return new ParserResult<>(result, errors);
    }

    static <T> ParserResult<T> fail(String error) {
      return new ParserResult<>(null, Collections.singletonList(error));
    }

    static <T> ParserResult<T> fail(List<String> errors) {
      return new ParserResult<>(null, errors);
    }

    Optional<T> getResult() {
      return Optional.ofNullable(result);
    }

    List<String> getErrors() {
      return errors;
    }
  }
}
