package org.gbif.registry.ws.mixins;

import org.gbif.api.jackson.LicenseSerde;
import org.gbif.api.vocabulary.License;

import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Jackson Mixin used to change the behavior of some fields on the Dataset object json representation in the context
 * of the registry-ws.
 */
public interface DatasetMixin {

  @JsonSerialize(using = LicenseSerde.LicenseJsonSerializer.class)
  @JsonDeserialize(using = LicenseSerde.LicenseJsonDeserializer.class)
  License getLicense();
}



