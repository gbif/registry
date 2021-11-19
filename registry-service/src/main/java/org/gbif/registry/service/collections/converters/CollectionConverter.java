package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.collections.MasterSourceType;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.PreservationMethodType;
import org.gbif.api.vocabulary.collections.PreservationType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.api.vocabulary.PreservationMethodType.ALCOHOL;
import static org.gbif.api.vocabulary.PreservationMethodType.DEEP_FROZEN;
import static org.gbif.api.vocabulary.PreservationMethodType.DRIED;
import static org.gbif.api.vocabulary.PreservationMethodType.DRIED_AND_PRESSED;
import static org.gbif.api.vocabulary.PreservationMethodType.FORMALIN;
import static org.gbif.api.vocabulary.PreservationMethodType.FREEZE_DRIED;
import static org.gbif.api.vocabulary.PreservationMethodType.GLYCERIN;
import static org.gbif.api.vocabulary.PreservationMethodType.GUM_ARABIC;
import static org.gbif.api.vocabulary.PreservationMethodType.MICROSCOPIC_PREPARATION;
import static org.gbif.api.vocabulary.PreservationMethodType.MOUNTED;
import static org.gbif.api.vocabulary.PreservationMethodType.NO_TREATMENT;
import static org.gbif.api.vocabulary.PreservationMethodType.OTHER;
import static org.gbif.api.vocabulary.PreservationMethodType.PINNED;
import static org.gbif.api.vocabulary.PreservationMethodType.REFRIGERATED;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_DRIED;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_FLUID_PRESERVED;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_FREEZE_DRYING;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_OTHER;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_PINNED;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_PRESSED;
import static org.gbif.api.vocabulary.collections.PreservationType.SAMPLE_SLIDE_MOUNT;
import static org.gbif.api.vocabulary.collections.PreservationType.STORAGE_FROZEN_BETWEEN_MINUS_132_AND_MINUS_196;
import static org.gbif.api.vocabulary.collections.PreservationType.STORAGE_OTHER;
import static org.gbif.api.vocabulary.collections.PreservationType.STORAGE_REFRIGERATED;
import static org.gbif.registry.service.collections.converters.ConverterUtils.convertAddress;
import static org.gbif.registry.service.collections.converters.ConverterUtils.normalizePunctuationSigns;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionConverter {

  private static final Map<PreservationMethodType, List<PreservationType>> PRESERVATION_METHOD_MAP =
      new EnumMap<>(PreservationMethodType.class);

  static {
    PRESERVATION_METHOD_MAP.put(NO_TREATMENT, Collections.emptyList());
    PRESERVATION_METHOD_MAP.put(ALCOHOL, Collections.singletonList(SAMPLE_FLUID_PRESERVED));
    PRESERVATION_METHOD_MAP.put(
        DEEP_FROZEN, Collections.singletonList(STORAGE_FROZEN_BETWEEN_MINUS_132_AND_MINUS_196));
    PRESERVATION_METHOD_MAP.put(DRIED, Collections.singletonList(SAMPLE_DRIED));
    PRESERVATION_METHOD_MAP.put(DRIED_AND_PRESSED, Arrays.asList(SAMPLE_DRIED, SAMPLE_PRESSED));
    PRESERVATION_METHOD_MAP.put(FORMALIN, Collections.singletonList(SAMPLE_FLUID_PRESERVED));
    PRESERVATION_METHOD_MAP.put(REFRIGERATED, Collections.singletonList(STORAGE_REFRIGERATED));
    PRESERVATION_METHOD_MAP.put(FREEZE_DRIED, Collections.singletonList(SAMPLE_FREEZE_DRYING));
    PRESERVATION_METHOD_MAP.put(GLYCERIN, Collections.singletonList(SAMPLE_FLUID_PRESERVED));
    PRESERVATION_METHOD_MAP.put(GUM_ARABIC, Collections.singletonList(SAMPLE_FLUID_PRESERVED));
    PRESERVATION_METHOD_MAP.put(
        MICROSCOPIC_PREPARATION, Collections.singletonList(SAMPLE_SLIDE_MOUNT));
    PRESERVATION_METHOD_MAP.put(MOUNTED, Collections.singletonList(SAMPLE_OTHER));
    PRESERVATION_METHOD_MAP.put(PINNED, Collections.singletonList(SAMPLE_PINNED));
    PRESERVATION_METHOD_MAP.put(OTHER, Collections.singletonList(STORAGE_OTHER));
  }

  private static List<PreservationType> fromPreservationMethodType(
      PreservationMethodType preservationMethodType) {
    return PRESERVATION_METHOD_MAP.getOrDefault(preservationMethodType, Collections.emptyList());
  }

  public static Collection convertFromDataset(
      Dataset dataset, Organization publisherOrganization, String collectionCode) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(collectionCode));

    Collection collection = new Collection();
    collection.setCode(collectionCode);
    return convertFromDataset(dataset, publisherOrganization, collection);
  }

  public static Collection convertFromDataset(
      Dataset dataset, Organization publisherOrganization, Collection existingCollection) {
    Objects.requireNonNull(dataset);
    Objects.requireNonNull(publisherOrganization);
    Objects.requireNonNull(existingCollection);

    existingCollection.setName(dataset.getTitle());
    existingCollection.setDescription(dataset.getDescription());
    existingCollection.setHomepage(dataset.getHomepage());
    existingCollection.setMasterSource(MasterSourceType.GBIF_REGISTRY);

    List<PreservationType> preservationTypes =
        dataset.getCollections().stream()
            .filter(c -> c.getSpecimenPreservationMethod() != null)
            .flatMap(c -> fromPreservationMethodType(c.getSpecimenPreservationMethod()).stream())
            .collect(Collectors.toList());
    existingCollection.setPreservationTypes(preservationTypes);

    Function<TaxonomicCoverages, String> taxonomicCoveragesToString =
        tc -> {
          StringBuilder sb = new StringBuilder();
          if (!Strings.isNullOrEmpty(tc.getDescription())) {
            sb.append(tc.getDescription().trim());

            if (!tc.getCoverages().isEmpty()) {
              sb.append(": ");
            }
          }

          tc.getCoverages()
              .forEach(
                  c -> {
                    boolean hasScientificName = false;
                    if (!Strings.isNullOrEmpty(c.getScientificName())) {
                      sb.append(c.getScientificName().trim());
                      hasScientificName = true;
                    }
                    if (!Strings.isNullOrEmpty(c.getCommonName())) {
                      if (hasScientificName) {
                        sb.append(", ");
                      }
                      sb.append(c.getCommonName().trim());
                    }
                  });

          return sb.toString();
        };

    String taxonomicCoverage =
        dataset.getTaxonomicCoverages().stream()
            .map(taxonomicCoveragesToString)
            .collect(Collectors.joining(";"));
    taxonomicCoverage = normalizePunctuationSigns(taxonomicCoverage).trim();
    existingCollection.setTaxonomicCoverage(taxonomicCoverage);

    String geographicCoverage =
        dataset.getGeographicCoverages().stream()
            .map(g -> g.getDescription().trim())
            .collect(Collectors.joining("."));
    geographicCoverage = normalizePunctuationSigns(geographicCoverage).trim();
    existingCollection.setGeography(geographicCoverage);

    existingCollection.setIncorporatedCollections(
        dataset.getCollections().stream()
            .map(org.gbif.api.model.registry.eml.Collection::getName)
            .collect(Collectors.toList()));

    existingCollection.setActive(true);

    existingCollection
        .getIdentifiers()
        .add(new Identifier(IdentifierType.DOI, dataset.getDoi().getDoiName()));

    existingCollection.setAddress(convertAddress(publisherOrganization));

    // contacts
    List<Contact> collectionContacts =
        dataset.getContacts().stream()
            .map(ConverterUtils::datasetContactToCollectionsContact)
            .collect(Collectors.toList());
    existingCollection.setContactPersons(collectionContacts);

    return existingCollection;
  }
}
