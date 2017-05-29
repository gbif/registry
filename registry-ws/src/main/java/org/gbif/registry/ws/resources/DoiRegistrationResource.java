package org.gbif.registry.ws.resources;

import org.gbif.api.model.common.DOI;
import org.gbif.api.model.common.DoiData;
import org.gbif.api.model.common.DoiStatus;
import org.gbif.doi.metadata.datacite.DataCiteMetadata;
import org.gbif.doi.metadata.datacite.DataCiteMetadata.AlternateIdentifiers;
import org.gbif.doi.service.InvalidMetadataException;
import org.gbif.doi.service.datacite.DataCiteValidator;
import org.gbif.registry.doi.DoiPersistenceService;
import org.gbif.registry.doi.DoiType;
import org.gbif.registry.doi.generator.DoiGenerator;
import org.gbif.registry.doi.registration.DoiRegistration;
import org.gbif.registry.doi.registration.DoiRegistrationService;
import org.gbif.ws.server.interceptor.NullToNotFound;
import org.gbif.ws.util.ExtraMediaTypes;

import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource class that exposes services to interact with DOI issued thru GBIF and DataCite.
 */
@Singleton
@Path("doi")
@Produces({MediaType.APPLICATION_JSON, ExtraMediaTypes.APPLICATION_JAVASCRIPT})
@Consumes(MediaType.APPLICATION_JSON)
public class DoiRegistrationResource implements DoiRegistrationService {

  private static final Logger LOG = LoggerFactory.getLogger(DoiRegistrationResource.class);

  private final DoiGenerator doiGenerator;
  private final DoiPersistenceService doiPersistenceService;

  @Inject(optional = true)
  @Named("guiceInjectedSecurityContext")
  @Context
  private SecurityContext securityContext;

  @Inject
  public DoiRegistrationResource(DoiGenerator doiGenerator, DoiPersistenceService doiPersistenceService) {
    this.doiGenerator = doiGenerator;
    this.doiPersistenceService = doiPersistenceService;
  }

  /**
   * Generates a new DOI based on the DoiType.
   */
  @POST
  @Path("gen/{type}")
  @Override
  public DOI generate(@NotNull @PathParam("type") DoiType doiType) {
    checkIsUserAuthenticated();
    return genDoiByType(doiType);
  }

  /**
   * Retrieves the DOI information.
   */
  @GET
  @Path("{prefix}/{suffix}")
  @NullToNotFound
  @Override
  public DoiData get(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix) {
    return doiPersistenceService.get(new DOI(prefix, suffix));
  }

  /**
   * Deletes an existent DOI.
   */
  @DELETE
  @Path("{prefix}/{suffix}")
  @NullToNotFound
  @Override
  public void delete(@PathParam("prefix") String prefix, @PathParam("suffix") String suffix) {
    LOG.info("Deleting DOI {} {}", prefix, suffix);
    doiGenerator.delete(new DOI(prefix, suffix));
  }

  /**
   * Register a new DOI, if the registration object doesn't contain a DOI a new DOI is generated.
   */
  @POST
  @NullToNotFound
  @Override
  public DOI register(DoiRegistration doiRegistration) {
    checkIsUserAuthenticated();
    try {
      //registration contains a DOI already
      DOI doi = doiRegistration.getDoi() == null ? genDoiByType(doiRegistration.getType()) : doiRegistration.getDoi();
      //Ensures that the metadata contains the DOI as an alternative identifier
      DataCiteMetadata dataCiteMetadata = DataCiteValidator.fromXml(doiRegistration.getMetadata());
      DataCiteMetadata metadata = DataCiteMetadata.copyOf(dataCiteMetadata)
                                    .withAlternateIdentifiers(
                                      addDoiToIdentifiers(dataCiteMetadata.getAlternateIdentifiers(), doi)).build();
      //handle registration
      if (DoiType.DATA_PACKAGE == doiRegistration.getType()) {
        doiGenerator.registerDataPackage(doi, metadata);
      } else if (DoiType.DOWNLOAD == doiRegistration.getType()) {
        doiGenerator.registerDownload(doi, metadata, doiRegistration.getKey());
      } else if (DoiType.DATASET == doiRegistration.getType()) {
        doiGenerator.registerDataset(doi, metadata,  UUID.fromString(doiRegistration.getKey()));
      }
      LOG.info("DOI registered {}", doi.getDoiName());
      return doi;
    } catch (InvalidMetadataException | JAXBException ex) {
      LOG.info("Error registering DOI", ex);
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Ensures that the DOI is included as AlternateIdentifier.
   */
  private static AlternateIdentifiers addDoiToIdentifiers(AlternateIdentifiers alternateIdentifiers, DOI doi) {
    AlternateIdentifiers.Builder<Void> builder = AlternateIdentifiers.builder();
    if (alternateIdentifiers != null && alternateIdentifiers.getAlternateIdentifier() != null) {
      builder.addAlternateIdentifier(alternateIdentifiers.getAlternateIdentifier().stream()
                                       .filter( identifier -> !identifier.getValue().equals(doi.getDoiName())
                                                              && !identifier.getAlternateIdentifierType()
                                                                   .equalsIgnoreCase("DOI"))
                                       .collect(Collectors.toList()));
    }
    builder.addAlternateIdentifier(AlternateIdentifiers.AlternateIdentifier.builder()
                                     .withValue(doi.getDoiName())
                                     .withAlternateIdentifierType("DOI")
                                     .build());
    return builder.build();
  }

  /**
   * Generates DOI based on the DoiType.
   */
  private DOI genDoiByType(DoiType doiType) {
    if (DoiType.DATA_PACKAGE == doiType) {
      return doiGenerator.newDataPackageDOI();
    } else if (DoiType.DOWNLOAD == doiType) {
      return doiGenerator.newDownloadDOI();
    } else {
      return doiGenerator.newDatasetDOI();
    }
  }

  /**
   * Check that the user is authenticated.
   */
  private void checkIsUserAuthenticated() {
     if(securityContext == null)
       throw new WebApplicationException(Response.Status.UNAUTHORIZED);
  }

}
