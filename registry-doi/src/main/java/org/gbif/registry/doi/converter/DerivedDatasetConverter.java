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

import java.util.Date;
import java.util.List;

import static org.gbif.registry.doi.util.DataCiteConstants.GBIF_PUBLISHER;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;


public final class DerivedDatasetConverter {

  private DerivedDatasetConverter() {
  }

  public static DataCiteMetadata convert(DOI doi, String creatorName, String title, List<DOI> relatedDatasets) {
    final DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, doi);
    convertCreators(builder, creatorName);
    convertTitles(builder, title);
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
    convertRelatedIdentifiers(builder, relatedDatasets);
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

  private static void convertCreators(DataCiteMetadata.Builder<Void> builder, String creatorName) {
    builder.withCreators(
        Creators.builder()
            .withCreator(
                Creator.builder()
                    .withCreatorName(CreatorName.builder().withValue(creatorName).build())
                    .build())
            .build());
  }

  private static void convertTitles(DataCiteMetadata.Builder<Void> builder, String title) {
    builder.withTitles(
        Titles.builder()
            .withTitle(Title.builder().withValue(title).build())
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
            .withResourceTypeGeneral(ResourceType.DATASET) // TODO: 27/08/2020 is DATASET ok?  what value?
            .build());
  }

  // TODO: 27/08/2020 is this ok?
  private static void convertRelatedIdentifiers(
      DataCiteMetadata.Builder<Void> builder, List<DOI> relatedDatasets) {
    if (!relatedDatasets.isEmpty()) {
      final RelatedIdentifiers.Builder<?> relBuilder = builder.withRelatedIdentifiers();
      for (DOI doi : relatedDatasets) {
        if (doi != null) {
          relBuilder.addRelatedIdentifier(
              RelatedIdentifier.builder()
                  .withRelationType(RelationType.IS_DERIVED_FROM)
                  .withValue(doi.getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
        }
      }
    }
  }
}
