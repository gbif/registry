package org.gbif.registry.service.collections.descriptors;

import static org.gbif.api.vocabulary.Kingdom.INCERTAE_SEDIS;
import static org.gbif.api.vocabulary.OccurrenceIssue.*;

import com.google.common.base.Strings;
import com.google.common.collect.Range;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.gbif.api.model.checklistbank.NameUsage;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.OccurrenceIssue;
import org.gbif.api.vocabulary.Rank;
import org.gbif.api.vocabulary.TypeStatus;
import org.gbif.checklistbank.ws.client.NubResourceClient;
import org.gbif.common.parsers.CountryParser;
import org.gbif.common.parsers.NumberParser;
import org.gbif.common.parsers.TypeStatusParser;
import org.gbif.common.parsers.core.OccurrenceParseResult;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.MultiinputTemporalParser;
import org.gbif.dwc.terms.DwcTerm;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Interpreter {

  private static final String DEFAULT_SEPARATOR = "\\|";
  private static final LocalDate EARLIEST_DATE_IDENTIFIED = LocalDate.of(1753, 1, 1);
  private static final Pattern INT_POSITIVE_PATTERN = Pattern.compile("(^\\d{1,10}$)");
  private static final MultiinputTemporalParser temporalParser = MultiinputTemporalParser.create();
  private static final CountryParser countryParser = CountryParser.getInstance();
  private static final TypeStatusParser typeStatusParser = TypeStatusParser.getInstance();

  public static InterpretedResult<List<String>> interpretStringList(
      String[] values, Map<String, Integer> headersByName, DwcTerm term) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    List<String> verbatimValue = extractListValue(values, headersByName, term);
    if (verbatimValue == null || verbatimValue.isEmpty()) {
      return InterpretedResult.empty();
    }

    return InterpretedResult.<List<String>>builder().result(verbatimValue).build();
  }

  public static InterpretedResult<String> interpretString(
      String[] values, Map<String, Integer> headersByName, DwcTerm term) {
    return interpretString(values, headersByName, term.prefixedName());
  }

  public static InterpretedResult<String> interpretString(
      String[] values, Map<String, Integer> headersByName, String fieldName) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    String verbatimValue = extractValue(values, headersByName, fieldName);
    if (Strings.isNullOrEmpty(verbatimValue)) {
      return InterpretedResult.empty();
    }

    return InterpretedResult.<String>builder().result(verbatimValue).build();
  }

  public static InterpretedResult<List<String>> interpretTypeStatus(
      String[] values, Map<String, Integer> headersByName) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    List<String> verbatimValue = extractListValue(values, headersByName, DwcTerm.typeStatus);
    if (verbatimValue == null || verbatimValue.isEmpty()) {
      return InterpretedResult.empty();
    }

    List<String> results = new ArrayList<>();
    Set<String> issues = new HashSet<>();
    verbatimValue.forEach(
        v -> {
          ParseResult<TypeStatus> parseResult = typeStatusParser.parse(v);
          if (parseResult.isSuccessful()) {
            results.add(parseResult.getPayload().name());
          } else {
            issues.add(TYPE_STATUS_INVALID.getId());
          }
        });

    return InterpretedResult.<List<String>>builder()
        .result(results)
        .issues(new ArrayList<>(issues))
        .build();
  }

  public static InterpretedResult<Date> interpretDateIdentified(
      String[] values, Map<String, Integer> headersByName) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    String verbatimDateIdentified = extractValue(values, headersByName, DwcTerm.dateIdentified);
    if (Strings.isNullOrEmpty(verbatimDateIdentified)) {
      return InterpretedResult.empty();
    }

    LocalDate upperBound = LocalDate.now().plusDays(1);
    Range<LocalDate> validRecordedDateRange = Range.closed(EARLIEST_DATE_IDENTIFIED, upperBound);
    OccurrenceParseResult<TemporalAccessor> parsed =
        temporalParser.parseLocalDate(
            verbatimDateIdentified,
            validRecordedDateRange,
            OccurrenceIssue.IDENTIFIED_DATE_UNLIKELY,
            OccurrenceIssue.IDENTIFIED_DATE_INVALID);

    InterpretedResult.InterpretedResultBuilder<Date> resultBuilder =
        InterpretedResult.<Date>builder()
            .issues(
                parsed.getIssues().stream()
                    .map(OccurrenceIssue::getId)
                    .collect(Collectors.toList()));

    if (parsed.isSuccessful()) {
      resultBuilder.result(new Date(Instant.from(parsed.getPayload()).toEpochMilli()));
    }

    return resultBuilder.build();
  }

  public static InterpretedResult<Integer> interpretIndividualCount(
      String[] values, Map<String, Integer> headersByName) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    String verbatimIndividualCount = extractValue(values, headersByName, DwcTerm.individualCount);
    if (Strings.isNullOrEmpty(verbatimIndividualCount)) {
      return InterpretedResult.empty();
    }

    boolean matches = INT_POSITIVE_PATTERN.matcher(verbatimIndividualCount).matches();
    if (!matches) {
      return InterpretedResult.<Integer>builder()
          .issues(Collections.singletonList(INDIVIDUAL_COUNT_INVALID.getId()))
          .build();
    }

    return InterpretedResult.<Integer>builder()
        .result(NumberParser.parseInteger(verbatimIndividualCount))
        .build();
  }

  public static InterpretedResult<Country> interpretCountry(
      String[] values, Map<String, Integer> headersByName) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    String verbatimCountry = extractValue(values, headersByName, DwcTerm.country);
    String verbatimCountryCode = extractValue(values, headersByName, DwcTerm.countryCode);

    if (Strings.isNullOrEmpty(verbatimCountry) && Strings.isNullOrEmpty(verbatimCountryCode)) {
      return InterpretedResult.empty();
    }

    Set<String> issues = new HashSet<>();
    Country interpretedCountry = null;
    if (!Strings.isNullOrEmpty(verbatimCountry)) {
      ParseResult<Country> parseResult = countryParser.parse(verbatimCountry);
      if (!parseResult.isSuccessful()) {
        issues.add(COUNTRY_INVALID.getId());
      } else {
        interpretedCountry = parseResult.getPayload();
      }
    }

    Country interpretedCountryCode = null;
    if (!Strings.isNullOrEmpty(verbatimCountryCode)) {
      ParseResult<Country> parseResult = countryParser.parse(verbatimCountryCode);
      if (!parseResult.isSuccessful()) {
        issues.add(COUNTRY_INVALID.getId());
      } else {
        interpretedCountryCode = parseResult.getPayload();
      }
    }

    if (interpretedCountry != null
        && interpretedCountryCode != null
        && interpretedCountry != interpretedCountryCode) {
      issues.add(COUNTRY_MISMATCH.getId());
    }

    InterpretedResult.InterpretedResultBuilder<Country> resultBuilder =
        InterpretedResult.<Country>builder().issues(new ArrayList<>(issues));
    if (interpretedCountry != null) {
      resultBuilder.result(interpretedCountry);
    } else if (interpretedCountryCode != null) {
      resultBuilder.result(interpretedCountryCode);
    }

    return resultBuilder.build();
  }

  public static InterpretedResult<TaxonData> interpretTaxonomy(
      String[] values, Map<String, Integer> headersByName, NubResourceClient nubResourceClient) {
    if (values.length == 0) {
      return InterpretedResult.empty();
    }

    String kingdom = extractValue(values, headersByName, DwcTerm.kingdom);
    String phylum = extractValue(values, headersByName, DwcTerm.phylum);
    String clazz = extractValue(values, headersByName, DwcTerm.class_);
    String order = extractValue(values, headersByName, DwcTerm.order);
    String family = extractValue(values, headersByName, DwcTerm.family);
    String genus = extractValue(values, headersByName, DwcTerm.genus);
    String scientificName = extractValue(values, headersByName, DwcTerm.scientificName);
    String genericName = extractValue(values, headersByName, DwcTerm.genericName);
    String specificEpithet = extractValue(values, headersByName, DwcTerm.specificEpithet);
    String infraspecificEpithet = extractValue(values, headersByName, DwcTerm.infraspecificEpithet);
    String scientificNameAuthorship =
        extractValue(values, headersByName, DwcTerm.scientificNameAuthorship);
    String taxonRank = extractValue(values, headersByName, DwcTerm.taxonRank);
    String taxonID = extractValue(values, headersByName, DwcTerm.taxonID);

    NameUsage nameUsageParam = new NameUsage();
    nameUsageParam.setKingdom(kingdom);
    nameUsageParam.setPhylum(phylum);
    nameUsageParam.setClazz(clazz);
    nameUsageParam.setOrder(order);
    nameUsageParam.setFamily(family);
    nameUsageParam.setGenus(genus);
    nameUsageParam.setTaxonID(taxonID);

    // TODO: also do backbone match by ID??
    NameUsageMatch2 nameUsageMatch =
        nubResourceClient.match2(
            scientificName,
            scientificNameAuthorship,
            taxonRank,
            genericName,
            specificEpithet,
            infraspecificEpithet,
            nameUsageParam,
            false,
            false);

    if (nameUsageMatch == null
        || isEmptyResponse(nameUsageMatch)
        || checkFuzzy(nameUsageMatch, nameUsageParam)) {

      return InterpretedResult.<TaxonData>builder()
          .result(
              TaxonData.builder()
                  .usageName(INCERTAE_SEDIS.scientificName())
                  .usageKey(INCERTAE_SEDIS.nubUsageKey())
                  .build())
          .issues(Collections.singletonList(TAXON_MATCH_NONE.getId()))
          .build();
    } else {
      NameUsageMatch.MatchType matchType = nameUsageMatch.getDiagnostics().getMatchType();

      List<String> issues = new ArrayList<>();
      if (NameUsageMatch.MatchType.NONE == matchType) {
        issues.add(TAXON_MATCH_NONE.getId());
      } else if (NameUsageMatch.MatchType.FUZZY == matchType) {
        issues.add(TAXON_MATCH_FUZZY.getId());
      } else if (NameUsageMatch.MatchType.HIGHERRANK == matchType) {
        issues.add(TAXON_MATCH_HIGHERRANK.getId());
      }

      TaxonData.TaxonDataBuilder taxonDataBuilder = TaxonData.builder();
      Set<Integer> taxonKeys = new HashSet<>();
      taxonDataBuilder.taxonKeys(taxonKeys);
      if (nameUsageMatch.getUsage() != null) {
        taxonDataBuilder
            .usageName(nameUsageMatch.getUsage().getName())
            .usageKey(nameUsageMatch.getUsage().getKey())
            .usageRank(nameUsageMatch.getUsage().getRank());
        taxonKeys.add(nameUsageMatch.getUsage().getKey());
      }

      if (nameUsageMatch.getClassification() != null) {
        nameUsageMatch
            .getClassification()
            .forEach(
                c -> {
                  taxonDataBuilder.rankedName(c);
                  taxonKeys.add(c.getKey());
                });
      }

      return InterpretedResult.<TaxonData>builder()
          .result(taxonDataBuilder.build())
          .issues(issues)
          .build();
    }
  }

  private static String extractValue(
      String[] values, Map<String, Integer> headersByName, DwcTerm term) {
    return extractValue(values, headersByName, term.prefixedName());
  }

  private static String extractValue(
      String[] values, Map<String, Integer> headersByName, String fieldName) {
    return Optional.ofNullable(headersByName.get(fieldName.toLowerCase()))
        .map(i -> values[i])
        .filter(v -> !v.isEmpty())
        .orElse(null);
  }

  private static Optional<String> extractOptValue(
      String[] values, Map<String, Integer> headersByName, DwcTerm term) {
    return Optional.ofNullable(headersByName.get(term.prefixedName().toLowerCase()))
        .map(i -> values[i])
        .filter(v -> !v.isEmpty());
  }

  private static List<String> extractListValue(
      String[] values, Map<String, Integer> headersByName, DwcTerm term) {
    return extractOptValue(values, headersByName, term)
        .map(
            x ->
                Arrays.stream(x.split(DEFAULT_SEPARATOR))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  private static boolean isEmptyResponse(NameUsageMatch2 response) {
    return response == null || response.getUsage() == null || response.getDiagnostics() == null;
  }

  private static boolean checkFuzzy(NameUsageMatch2 usageMatch, NameUsage nameUsageParam) {
    boolean isFuzzy = NameUsageMatch.MatchType.FUZZY == usageMatch.getDiagnostics().getMatchType();
    boolean isEmptyTaxa =
        Strings.isNullOrEmpty(nameUsageParam.getKingdom())
            && Strings.isNullOrEmpty(nameUsageParam.getPhylum())
            && Strings.isNullOrEmpty(nameUsageParam.getClazz())
            && Strings.isNullOrEmpty(nameUsageParam.getOrder())
            && Strings.isNullOrEmpty(nameUsageParam.getFamily());
    return isFuzzy && isEmptyTaxa;
  }

  @Data
  @Builder
  static class TaxonData {
    private Integer usageKey;
    private String usageName;
    private Rank usageRank;

    @Singular("rankedName")
    private List<RankedName> taxonClassification;

    private Set<Integer> taxonKeys;
  }
}
