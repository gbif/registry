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
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.Titles.Title;
import org.gbif.doi.metadata.datacite.ResourceType;
import org.gbif.registry.domain.ws.DerivedDataset;

import java.util.Date;

import static org.gbif.registry.doi.util.DataCiteConstants.GBIF_PUBLISHER;
import static org.gbif.registry.doi.util.RegistryDoiUtils.getYear;

public final class DerivedDatasetConverter {

  private DerivedDatasetConverter() {}

  public static DataCiteMetadata convert(DerivedDataset derivedDataset) {
    final DataCiteMetadata.Builder<Void> builder = DataCiteMetadata.builder();

    // Required fields
    convertIdentifier(builder, derivedDataset);
    convertCreators(builder, derivedDataset);
    convertTitles(builder, derivedDataset);
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
    //    convertRelatedIdentifiers(builder);
    //    convertRightsList(builder);
    //    convertSubjects(builder);
    //    convertGeoLocations(builder);

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

  // TODO: 03/09/2020 convert related identifiers
}
