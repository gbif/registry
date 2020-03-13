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
package org.gbif.registry.utils.cucumber;

import org.gbif.api.model.collections.Address;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.common.GbifUser;
import org.gbif.api.model.pipelines.PipelineExecution;
import org.gbif.api.model.pipelines.PipelineStep;
import org.gbif.api.model.pipelines.ws.PipelineProcessParameters;
import org.gbif.api.model.pipelines.ws.PipelineStepParameters;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Identifier;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.model.registry.Tag;
import org.gbif.registry.domain.ws.LegacyDatasetResponse;
import org.gbif.registry.domain.ws.LegacyEndpoint;
import org.gbif.registry.domain.ws.LegacyEndpointResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationResponse;
import org.gbif.registry.identity.model.LoggedUserWithToken;

import java.util.Locale;

import io.cucumber.core.api.TypeRegistry;
import io.cucumber.core.api.TypeRegistryConfigurer;
import io.cucumber.datatable.DataTableType;

public class CucumberTypeRegistryConfigurer implements TypeRegistryConfigurer {

  @Override
  public Locale locale() {
    return Locale.ENGLISH;
  }

  @Override
  public void configureTypeRegistry(TypeRegistry typeRegistry) {
    typeRegistry.defineDataTableType(
        new DataTableType(Dataset.class, new DatasetTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(Contact.class, new ContactTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(Endpoint.class, new EndpointTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            LoggedUserWithToken.class, new LoggedUserWithTokenTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(Installation.class, new InstallationTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            LegacyOrganizationResponse.class,
            new LegacyOrganizationResponseTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            LegacyOrganizationBriefResponse.class,
            new LegacyOrganizationBriefResponseTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(LegacyEndpoint.class, new LegacyEndpointTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            LegacyEndpointResponse.class, new LegacyEndpointResponseTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            LegacyDatasetResponse.class, new LegacyDatasetResponseTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(GbifUser.class, new GbifUserTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            PipelineProcessParameters.class, new PipelineProcessParametersTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(PipelineExecution.class, new PipelineExecutionTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(PipelineStep.class, new PipelineStepTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(
            PipelineStepParameters.class, new PipelineStepParametersTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(Institution.class, new InstitutionTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(Address.class, new AddressTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(Tag.class, new TagTableEntryTransformer()));
    typeRegistry.defineDataTableType(
        new DataTableType(Identifier.class, new IdentifierTableEntryTransformer()));
  }
}
