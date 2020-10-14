/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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
package org.gbif.registry.doi.converter;

import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Creators.Creator.CreatorName;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Identifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Publisher;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.RelatedIdentifiers.RelatedIdentifier;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.RelatedIdentifierType;
import org.gbif.doi.metadata.datacite.RelationType;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.registry.domain.ws.DerivedDataset;
import org.gbif.registry.domain.ws.DerivedDatasetUsage;

import java.util.Date;
import java.util.List;

import static org.gbif.registry.doi.util.DataCiteConstants.GBIF_PUBLISHER;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;

public final class DerivedDatasetConverter {

  private DerivedDatasetConverter() {}

  public static DataCiteMetadata convert(
      DerivedDataset derivedDataset, List<DerivedDatasetUsage> derivedDatasetUsages) {
    final DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, derivedDataset);
    convertCreators(builder, derivedDataset);
    convertTitles(builder, derivedDataset);
    convertPublisher(builder);
    convertPublicationYear(builder);
    convertResourceType(builder);

    // Optional and recommended fields
    convertRelatedIdentifiers(builder, derivedDataset, derivedDatasetUsages);

    return builder.build();
  }

  private static void convertIdentifier(
      DataCiteMetadata.Builder<Void> builder, DerivedDataset derivedDataset) {
    builder.withIdentifier(
        Identifier.builder()
            .withIdentifierType(IdentifierType.DOI.name())
            .withValue(derivedDataset.getDoi().getDoiName())
            .build());
  }

  private static void convertCreators(
      DataCiteMetadata.Builder<Void> builder, DerivedDataset derivedDataset) {
    builder.withCreators(
        Creators.builder()
            .withCreator(
                Creator.builder()
                    .withCreatorName(
                        CreatorName.builder().withValue(derivedDataset.getCreatedBy()).build())
                    .build())
            .build());
  }

  private static void convertTitles(
      DataCiteMetadata.Builder<Void> builder, DerivedDataset derivedDataset) {
    builder.withTitles(
        Titles.builder()
            .withTitle(Title.builder().withValue(derivedDataset.getTitle()).build())
            .build());
  }

  private static void convertPublisher(DataCiteMetadata.Builder<Void> builder) {
    builder.withPublisher(Publisher.builder().withValue(GBIF_PUBLISHER).build());
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
      DataCiteMetadata.Builder<Void> builder,
      DerivedDataset derivedDataset,
      List<DerivedDatasetUsage> datasetUsages) {
    // include related datasets
    if (!datasetUsages.isEmpty()) {
      for (DerivedDatasetUsage du : datasetUsages) {
        if (du.getDatasetDoi() != null) {
          builder
              .withRelatedIdentifiers()
              .addRelatedIdentifier(
                  RelatedIdentifier.builder()
                      .withRelationType(RelationType.REFERENCES)
                      .withValue(du.getDatasetDoi().getDoiName())
                      .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                      .build());
        }
      }
    }

    // include original download DOI if present
    if (derivedDataset.getOriginalDownloadDOI() != null) {
      builder
          .withRelatedIdentifiers()
          .addRelatedIdentifier(
              RelatedIdentifier.builder()
                  .withRelationType(RelationType.IS_DERIVED_FROM)
                  .withValue(derivedDataset.getOriginalDownloadDOI().getDoiName())
                  .withRelatedIdentifierType(RelatedIdentifierType.DOI)
                  .build());
    }
  }
}
