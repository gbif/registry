package org.gbif.registry.utils.cucumber;

import io.cucumber.core.api.TypeRegistry;
import io.cucumber.core.api.TypeRegistryConfigurer;
import io.cucumber.datatable.DataTableType;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.model.registry.Installation;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.model.LegacyEndpoint;
import org.gbif.registry.ws.model.LegacyOrganizationBriefResponse;
import org.gbif.registry.ws.model.LegacyOrganizationResponse;

import java.util.Locale;

public class CucumberTypeRegistryConfigurer implements TypeRegistryConfigurer {

  @Override
  public Locale locale() {
    return Locale.ENGLISH;
  }

  @Override
  public void configureTypeRegistry(TypeRegistry typeRegistry) {
    typeRegistry.defineDataTableType(new DataTableType(Dataset.class, new DatasetTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(Contact.class, new ContactTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(Endpoint.class, new EndpointTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(LoggedUserWithToken.class, new LoggedUserWithTokenTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(Installation.class, new InstallationTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(LegacyOrganizationResponse.class, new LegacyOrganizationResponseTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(LegacyOrganizationBriefResponse.class, new LegacyOrganizationBriefResponseTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(LegacyEndpoint.class, new LegacyEndpointTableEntryTransformer()));
  }
}
