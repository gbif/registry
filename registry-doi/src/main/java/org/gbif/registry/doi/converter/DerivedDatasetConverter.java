package org.gbif.registry.doi.converter;

import org.gbif.api.model.common.DOI;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Identifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.registry.doi.util.RegistryDoiUtils;
import org.gbif.registry.domain.ws.CitationCreationRequest;

import java.util.Date;

import static org.gbif.registry.doi.util.DataCiteConstants.GBIF_PUBLISHER;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;


public final class DerivedDatasetConverter {

  private DerivedDatasetConverter() {
  }

  public static DataCiteMetadata convert(DOI doi, CitationCreationRequest data) {
    final DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, doi);
    convertCreators(builder, data);
    convertTitles(builder, data);
    convertPublisher(builder);
    convertPublicationYear(builder);
    convertResourceType(builder);

    // Optional and recommended fields
    // TODO: 27/08/2020 which ones do we need?
//    convertDates(builder);
//    convertDescriptions(builder);
//    convertLanguage(builder);
//    convertContributors(builder);
//    convertAlternateIdentifiers(builder);
    convertRelatedIdentifiers(builder, data);
//    convertRightsList(builder);
//    convertSubjects(builder);
//    convertGeoLocations(builder);

    return builder.build();
  }

  private static void convertIdentifier(DataCiteMetadata.Builder<Void> builder, DOI doi) {
    builder.withIdentifier(
        Identifier.builder()
            .withIdentifierType(IdentifierType.DOI.name())
            .withValue(doi.getDoiName())
            .build());
  }

  private static void convertCreators(DataCiteMetadata.Builder<Void> builder, CitationCreationRequest data) {
    builder.withCreators(
        Creators.builder()
            .withCreator(
                Creator.builder()
                    .withCreatorName(CreatorName.builder().withValue(data.getCreator()).build())
                    .build())
            .build());
  }

  private static void convertTitles(DataCiteMetadata.Builder<Void> builder, CitationCreationRequest data) {
    builder.withTitles(
        Titles.builder()
            .withTitle(Title.builder().withValue(data.getTitle()).build())
            .build());
  }

  private static void convertPublisher(DataCiteMetadata.Builder<Void> builder) {
    // TODO: 27/08/2020 who is supposed to be the publisher?
    builder.withPublisher(DataCiteMetadata.Publisher.builder().withValue(GBIF_PUBLISHER).build());
  }

  private static void convertPublicationYear(DataCiteMetadata.Builder<Void> builder) {
    builder.withPublicationYear(getYear(new Date()));
  }

  private static void convertResourceType(DataCiteMetadata.Builder<Void> builder) {
    builder.withResourceType(
        DataCiteMetadata.ResourceType.builder()
            .withResourceTypeGeneral(ResourceType.DATASET)
            .build());
  }

  private static void convertRelatedIdentifiers(
      DataCiteMetadata.Builder<Void> builder, CitationCreationRequest data) {
    final RelatedIdentifiers.Builder<?> relBuilder = builder.withRelatedIdentifiers();

    if (data.getOriginalDownloadDOI() != null) {
      relBuilder.addRelatedIdentifier(
          RelatedIdentifier.builder()
              .withRelationType(RelationType.IS_DERIVED_FROM)
              .withValue(data.getOriginalDownloadDOI().getDoiName())
              .withRelatedIdentifierType(RelatedIdentifierType.DOI)
              .build());
    }

    if (!data.getRelatedDatasets().isEmpty()) {
      for (String doiOrUuid : data.getRelatedDatasets()) {
        if (doiOrUuid != null) {
          if (DOI.isParsable(doiOrUuid)) {
            relBuilder.addRelatedIdentifier(
                RelatedIdentifier.builder()
                    .withRelationType(RelationType.IS_DERIVED_FROM)
                    .withValue(new DOI(doiOrUuid).getDoiName())
                    .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                    .build());
          } else if (RegistryDoiUtils.isUuid(doiOrUuid)) {
            relBuilder.addRelatedIdentifier(
                RelatedIdentifier.builder()
                    .withRelationType(RelationType.IS_DERIVED_FROM)
                    .withValue("https://www.gbif.org/dataset/" + doiOrUuid)
                    .withRelatedIdentifierType(RelatedIdentifierType.URL)
                    .build());
          }
        }
      }
    }
  }
}
