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
package org.gbif.registry.utils;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.registry.Citation;
import org.gbif.api.model.registry.Comment;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.MachineTag;
import org.gbif.api.model.registry.Tag;
import org.gbif.api.model.registry.eml.Collection;
import org.gbif.api.model.registry.eml.DataDescription;
import org.gbif.api.model.registry.eml.KeywordCollection;
import org.gbif.api.model.registry.eml.Project;
import org.gbif.api.model.registry.eml.SamplingDescription;
import org.gbif.api.model.registry.eml.TaxonomicCoverages;
import org.gbif.api.model.registry.eml.curatorial.CuratorialUnitComposite;
import org.gbif.api.model.registry.eml.curatorial.CuratorialUnitCount;
import org.gbif.api.model.registry.eml.geospatial.GeospatialCoverage;
import org.gbif.api.model.registry.eml.temporal.DateRange;
import org.gbif.api.model.registry.eml.temporal.SingleDate;
import org.gbif.api.model.registry.eml.temporal.TemporalCoverage;
import org.gbif.api.model.registry.eml.temporal.VerbatimTimePeriod;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.Language;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.MaintenanceUpdateFrequency;
import org.gbif.registry.guice.RegistryTestModules;
import org.gbif.registry.ws.resources.DatasetResource;

import java.util.UUID;

import com.google.inject.Injector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class Datasets extends JsonBackedData<Dataset> {

  private static final Datasets INSTANCE = new Datasets();
  private static DatasetService datasetService;

  public static final String DATASET_ALIAS = "BGBM";
  public static final String DATASET_ABBREVIATION = "BGBM";
  public static final Language DATASET_LANGUAGE = Language.DANISH;
  public static final String DATASET_RIGHTS = "The rights";
  public static final License DATASET_LICENSE = License.CC_BY_NC_4_0;
  public static final DOI DATASET_DOI = new DOI(DOI.TEST_PREFIX, "gbif.2014.XSD123");
  public static final Citation DATASET_CITATION = new Citation("This is a citation text", "ABC");

  public Datasets() {
    super("data/dataset.json", new TypeReference<Dataset>() {
    });
    Injector i = RegistryTestModules.webservice();
    datasetService = i.getInstance(DatasetResource.class);
  }

  public static Dataset newInstance(UUID publishingOrganizationKey, UUID installationKey) {
    Dataset d = INSTANCE.newTypedInstance();
    d.setPublishingOrganizationKey(publishingOrganizationKey);
    d.setInstallationKey(installationKey);
    d.setDoi(DATASET_DOI);
    d.setLicense(DATASET_LICENSE);
    return d;
  }

  /**
   * Persist a new Dataset associated to an publishing organization and installation for use in Unit Tests.
   *
   * @param organizationKey publishing organization key
   * @param installationKey installation key
   * @return persisted Dataset
   */
  public static Dataset newPersistedInstance(UUID organizationKey, UUID installationKey) {
    Dataset dataset = Datasets.newInstance(organizationKey, installationKey);
    UUID key = datasetService.create(dataset);
    // some properties like created, modified are only set when the dataset is retrieved anew
    return datasetService.get(key);
  }

  /**
   * A utility to dump a Dataset as a JSON object to help Node developers see what they might expect.
  public static void main(String[] args) throws Exception {
    Dataset d = Datasets.newInstance(UUID.randomUUID(), UUID.randomUUID());
    d.getContacts().add(Contacts.newInstance());
    d.addEndpoint(Endpoints.newInstance());
    d.addMachineTag(MachineTags.newInstance());
    d.getBibliographicCitations().add(new Citation());
    d.getCollections().add(new Collection());
    d.getComments().add(new Comment());
    d.getCuratorialUnits().add(new CuratorialUnitComposite());
    d.getBibliographicCitations().add(new Citation());
    d.getCountryCoverage().add(Country.AFGHANISTAN);
    d.getDataDescriptions().add(new DataDescription());
    d.getGeographicCoverages().add(new GeospatialCoverage());
    d.getIdentifiers().add(new Identifier());
    d.getKeywordCollections().add(new KeywordCollection());
    d.setMaintenanceUpdateFrequency(MaintenanceUpdateFrequency.DAILY);
    d.setProject(new Project());
    d.setSamplingDescription(new SamplingDescription());
    d.getTags().add(new Tag());
    d.getTaxonomicCoverages().add(new TaxonomicCoverages());
    d.getTemporalCoverages().add(new DateRange());
    d.getTemporalCoverages().add(new SingleDate());
    d.getTemporalCoverages().add(new VerbatimTimePeriod());


    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, d);
    String json = mapper.writeValueAsString(d);
  }
  */

}
