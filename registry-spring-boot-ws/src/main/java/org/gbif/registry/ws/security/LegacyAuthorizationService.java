package org.gbif.registry.ws.security;

import org.gbif.ws.security.LegacyRequestAuthorization;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public interface LegacyAuthorizationService {

  LegacyRequestAuthorization authenticate(HttpServletRequest httpRequest);

  boolean isAuthorizedToModifyOrganization(LegacyRequestAuthorization authorization);

  boolean isAuthorizedToModifyOrganization(LegacyRequestAuthorization authorization, UUID organizationKey);

  boolean isAuthorizedToModifyOrganizationsDataset(LegacyRequestAuthorization authorization, UUID datasetKey);

  boolean isAuthorizedToModifyInstallation(LegacyRequestAuthorization authorization, UUID installationKey);
}
