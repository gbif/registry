package org.gbif.registry.utils.cucumber;

import io.cucumber.core.api.TypeRegistry;
import io.cucumber.core.api.TypeRegistryConfigurer;
import io.cucumber.datatable.DataTableType;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Endpoint;
import org.gbif.api.service.common.LoggedUserWithToken;
import org.gbif.registry.ws.model.LegacyDataset;

import java.util.Locale;

public class CucumberTypeRegistryConfigurer implements TypeRegistryConfigurer {

  @Override
  public Locale locale() {
    return Locale.ENGLISH;
  }

  @Override
  public void configureTypeRegistry(TypeRegistry typeRegistry) {
    typeRegistry.defineDataTableType(new DataTableType(LegacyDataset.class, new LegacyDatasetTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(Contact.class, new ContactTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(Endpoint.class, new EndpointTableEntryTransformer()));
    typeRegistry.defineDataTableType(new DataTableType(LoggedUserWithToken.class, new LoggedUserWithTokenTableEntryTransformer()));
  }
}
