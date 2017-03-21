package org.gbif.registry.doi.registration;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.registry.doi.DoiType;

/**
 * Specifies the contract of a service that manages DOI registrations.
 */
public interface DoiRegistrationService {

  /**
   * Generates a new DOI based on the DoiType.
   */
  DOI generate(DoiType doiType);

  /**
   * Retrieves the DOI information.
   */
  DoiData get(String prefix, String suffix);

  /**
   * Register a new DOI, if the registration object doesn't contain a DOI a new DOI is generated.
   */
  DOI register(DoiRegistration doiRegistration);


  /**
   * Deletes a DOI.
   */
  void delete(String prefix, String suffix);
}
