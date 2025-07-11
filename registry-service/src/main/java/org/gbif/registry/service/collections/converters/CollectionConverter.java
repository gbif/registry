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
package org.gbif.registry.service.collections.converters;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Organization;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.vocabulary.ContactType;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.api.vocabulary.PreservationMethodType;
import org.gbif.registry.service.collections.utils.Vocabularies;
import org.gbif.vocabulary.api.ConceptListParams;
import org.gbif.vocabulary.client.ConceptClient;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.registry.service.collections.converters.ConverterUtils.convertAddress;
import static org.gbif.registry.service.collections.converters.ConverterUtils.normalizePunctuationSigns;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionConverter {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static List<String> fromPreservationMethodType(
      PreservationMethodType preservationMethodType, ConceptClient conceptClient) {
    return conceptClient
        .listConceptsLatestRelease(
            Vocabularies.PRESERVATION_TYPE,
            ConceptListParams.builder().hiddenLabel(preservationMethodType.name()).build())
        .getResults()
        .stream()
        .map(cv -> cv.getConcept().getName())
        .collect(Collectors.toList());
  }

  public static Collection convertFromDataset(
      Dataset dataset,
      Organization publisherOrganization,
      String collectionCode,
      ConceptClient conceptClient) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(collectionCode));

    Collection collection = new Collection();
    collection.setCode(collectionCode);
    return convertFromDataset(dataset, publisherOrganization, collection, conceptClient);
  }

  public static Collection convertFromDataset(
      Dataset dataset,
      Organization publisherOrganization,
      Collection existingCollection,
      ConceptClient conceptClient) {
    Objects.requireNonNull(dataset);
    Objects.requireNonNull(publisherOrganization);
    Objects.requireNonNull(existingCollection);

    existingCollection.setName(dataset.getTitle());
    existingCollection.setDescription(dataset.getDescription());
    existingCollection.setHomepage(dataset.getHomepage());

    List<String> preservationTypes =
        dataset.getCollections().stream()
            .filter(c -> c.getSpecimenPreservationMethod() != null)
            .flatMap(
                c ->
                    fromPreservationMethodType(c.getSpecimenPreservationMethod(), conceptClient)
                        .stream())
            .collect(Collectors.toList());
    existingCollection.setPreservationTypes(preservationTypes);

    Function<TaxonomicCoverages, String> taxonomicCoveragesToString =
        tc -> {
          StringBuilder sb = new StringBuilder();
          if (!Strings.isNullOrEmpty(tc.getDescription())) {
            sb.append(tc.getDescription().trim());
          }

          String joinedCoverages =
              tc.getCoverages().stream()
                  .map(
                      c -> {
                        StringBuilder nameBuilder = new StringBuilder();
                        if (!Strings.isNullOrEmpty(c.getScientificName())) {
                          nameBuilder.append(c.getScientificName().trim());
                        }
                        if (!Strings.isNullOrEmpty(c.getCommonName())) {
                          if (nameBuilder.length() > 0) {
                            nameBuilder.append(", ");
                          }
                          nameBuilder.append(c.getCommonName().trim());
                        }
                        return nameBuilder.toString();
                      })
                  .filter(s -> !s.isEmpty())
                  .collect(Collectors.joining("; "));

          if (!joinedCoverages.isEmpty()) {
            if (sb.length() > 0) {
              sb.append(": ");
            }
            sb.append(joinedCoverages);
          }

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
    existingCollection.setGeographicCoverage(geographicCoverage);

    String temporalCoverage =
        dataset.getTemporalCoverages().stream()
            .map(
                tc -> {
                  if (tc instanceof DateRange) {
                    DateRange dr = (DateRange) tc;
                    return String.format(
                        "%s - %s",
                        dr.getStart() != null ? DATE_FORMAT.format(dr.getStart()) : "",
                        dr.getEnd() != null ? DATE_FORMAT.format(dr.getEnd()) : "");
                  } else if (tc instanceof SingleDate) {
                    SingleDate sd = (SingleDate) tc;
                    return sd.getDate() != null ? DATE_FORMAT.format(sd.getDate()) : "";
                  } else if (tc instanceof VerbatimTimePeriod) {
                    VerbatimTimePeriod vtp = (VerbatimTimePeriod) tc;
                    return vtp.getPeriod() != null ? vtp.getPeriod() : "";
                  }
                  return "";
                })
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining("; "));
    temporalCoverage = normalizePunctuationSigns(temporalCoverage).trim();
    existingCollection.setTemporalCoverage(temporalCoverage);

    existingCollection.setIncorporatedCollections(
        dataset.getCollections().stream()
            .map(org.gbif.api.model.registry.eml.Collection::getName)
            .collect(Collectors.toList()));

    existingCollection.setActive(true);

    if (existingCollection.getIdentifiers().stream()
        .noneMatch(
            i ->
                i.getType() == IdentifierType.DOI
                    && i.getIdentifier().equals(dataset.getDoi().getDoiName()))) {
      existingCollection
          .getIdentifiers()
          .add(new Identifier(IdentifierType.DOI, dataset.getDoi().getDoiName()));
    }

    existingCollection.setAddress(
        convertAddress(publisherOrganization, existingCollection.getAddress()));

    // contacts
    List<Contact> collectionContacts =
        dataset.getContacts().stream()
            .filter(
                c ->
                    c.getType() != ContactType.PROGRAMMER
                        && c.getType() != ContactType.METADATA_AUTHOR)
            .map(ConverterUtils::datasetContactToCollectionsContact)
            .collect(Collectors.toList());
    existingCollection.setContactPersons(collectionContacts);

    return existingCollection;
  }
}
