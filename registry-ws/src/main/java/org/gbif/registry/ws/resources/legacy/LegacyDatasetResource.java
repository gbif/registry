package org.gbif.registry.ws.resources.legacy;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ws.model.ErrorResponse;
import org.gbif.registry.ws.model.LegacyDataset;
import org.gbif.registry.ws.model.LegacyDatasetResponse;
import org.gbif.registry.ws.security.LegacyAuthorizationFilter;
import org.gbif.registry.ws.util.LegacyResourceConstants;
import org.gbif.registry.ws.util.LegacyResourceUtils;

import java.util.List;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.core.InjectParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle all legacy web service Dataset requests (excluding IPT requests), previously handled by the GBRDS.
 */
@Singleton
@Path("registry/resource")
public class LegacyDatasetResource {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyDatasetResource.class);

  private final OrganizationService organizationService;
  private final DatasetService datasetService;
  private final InstallationService installationService;
  private final IptResource iptResource;

  @Inject
  public LegacyDatasetResource(OrganizationService organizationService, DatasetService datasetService,
    IptResource iptResource, InstallationService installationService) {
    this.organizationService = organizationService;
    this.datasetService = datasetService;
    this.iptResource = iptResource;
    this.installationService = installationService;
  }


  /**
   * Register GBRDS dataset, handling incoming request with path /resource. The primary contact, publishing organization
   * key, and resource name are mandatory. Only after both the dataset and primary contact have been persisted is a
   * Response with Status.CREATED (201) returned.
   *
   * @param dataset IptDataset with HTTP form parameters having been injected from Jersey
   * @param security SecurityContext (security related information)
   * @return Response
   * @see IptResource#registerDataset(org.gbif.registry.ws.model.LegacyDataset, javax.ws.rs.core.SecurityContext)
   */
  @POST
  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response registerDataset(@InjectParam LegacyDataset dataset, @Context SecurityContext security) {
    // reuse existing subresource
    return iptResource.registerDataset(dataset, security);
  }

  /**
   * Update GBRDS Dataset, handling incoming request with path /resource/{key}. The publishing organization key is
   * mandatory (supplied in the credentials not the parameters). The primary contact is not required, but if any
   * of the primary contact parameters were included in the request, it is required. This is the difference between this
   * method and registerDataset. Only after both the dataset and optional primary contact have been updated is a
   * Response with Status.OK (201) returned.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @param dataset IptDataset with HTTP form parameters having been injected from Jersey
   * @param security SecurityContext (security related information)
   * @return Response with Status.CREATED (201) if successful
   */
  @POST
  @Path("{key}")
  @Produces(MediaType.APPLICATION_XML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response updateDataset(@PathParam("key") UUID datasetKey, @InjectParam LegacyDataset dataset,
    @Context SecurityContext security) {
    if (dataset != null) {
      // set required fields
      String user = security.getUserPrincipal().getName();
      dataset.setCreatedBy(user);
      dataset.setModifiedBy(user);
      dataset.setKey(datasetKey);
      // retrieve existing dataset
      Dataset existing = datasetService.get(datasetKey);
      // populate dataset with existing primary contact so it gets updated, not duplicated
      dataset.setContacts(existing.getContacts());
      // if primary contact wasn't supplied, set existing one here so that it doesn't respond BAD_REQUEST
      if (dataset.getPrimaryContactAddress() == null && dataset.getPrimaryContactEmail() == null
        && dataset.getPrimaryContactType() == null && dataset.getPrimaryContactPhone() == null
        && dataset.getPrimaryContactName() == null && dataset.getPrimaryContactDescription() == null) {
        dataset.setPrimaryContact(LegacyResourceUtils.getPrimaryContact(existing));
      }
      // otherwise, update primary contact and type
      else {
        dataset.prepare();
      }
      // If installation key wasn't provided, reuse existing dataset's installation key
      // Reason: non-IPT consumers weren't aware they could supply the parameter iptKey on dataset updates before
      if (dataset.getInstallationKey() == null) {
        dataset.setInstallationKey(existing.getInstallationKey());
      }
      // Dataset can only have 1 installation key, log if the hosting installation is being changed
      else if (dataset.getInstallationKey() != existing.getInstallationKey()) {
        LOG.debug("The dataset's technical installation is being changed from {} to {}", dataset.getInstallationKey(), existing.getInstallationKey());
      }
      // type can't be derived from endpoints, since there are no endpoints supplied on this update, so re-set existing
      dataset.setType(existing.getType());
      // populate publishing organization from credentials
      dataset.setPublishingOrganizationKey( LegacyAuthorizationFilter.extractOrgKeyFromSecurity(security) );
      // ensure the publishing organization exists, the installation exists, primary contact exists, etc
      Contact contact = dataset.getPrimaryContact();
      if (contact != null && LegacyResourceUtils.isValidOnUpdate(dataset,
                                datasetService, organizationService, installationService)) {
        // update only fields that could have changed
        existing.setModifiedBy(user);
        existing.setTitle(dataset.getTitle());
        existing.setDescription(dataset.getDescription());
        existing.setHomepage(dataset.getHomepage());
        existing.setLogoUrl(dataset.getLogoUrl());
        existing.setLanguage(dataset.getLanguage());
        existing.setInstallationKey(dataset.getInstallationKey());

        existing.setPublishingOrganizationKey(dataset.getPublishingOrganizationKey());

        // persist changes
        datasetService.update(existing);

        // set primary contact's required field(s)
        contact.setModifiedBy(user);
        // add/update primary contact: Contacts are mutable, so try to update if the Contact already exists
        if (contact.getKey() == null) {
          contact.setCreatedBy(user);
          datasetService.addContact(datasetKey, contact);
        } else {
          datasetService.updateContact(datasetKey, contact);
        }

        // endpoint changes are done through Service API

        LOG.info("Dataset updated successfully, key=%s", datasetKey.toString());
        return Response.status(Response.Status.CREATED).entity(dataset)
          .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();

      } else {
        LOG.error("Request invalid. Dataset missing required fields or using stale keys!");
      }
    }
    LOG.error("Dataset update failed");
    return Response.status(Response.Status.BAD_REQUEST).cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED)
      .build();
  }

  /**
   * Retrieve all Datasets owned by an organization, handling incoming request with path /resource.
   * The publishing organization query parameter is mandatory. Only after both
   * the organizationKey is verified to correspond to an existing organization, is a Response including the list of
   * Datasets returned.
   *
   * @param organizationKey organization key (UUID) coming in as query param
   * @return Response with list of Datasets or empty list with error message if none found
   */
  @GET
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Consumes(MediaType.TEXT_PLAIN)
  public Response datasetsForOrganization(@QueryParam("organisationKey") UUID organizationKey) {

    if (organizationKey != null) {
      try {
        LOG.debug("Get all Datasets owned by Organization, key={}", organizationKey);
        // verify organization with key exists, otherwise NotFoundException gets thrown
        organizationService.get(organizationKey);

        List<LegacyDatasetResponse> datasets = Lists.newArrayList();
        PagingRequest page = new PagingRequest(0, LegacyResourceConstants.WS_PAGE_SIZE);
        PagingResponse<Dataset> response;
        do {
          LOG.debug("Requesting {} datasets starting at offset {}", page.getLimit(), page.getOffset());
          response = organizationService.publishedDatasets(organizationKey, page);
          for (Dataset d : response.getResults()) {
            Contact contact = LegacyResourceUtils.getPrimaryContact(d);
            datasets.add(new LegacyDatasetResponse(d, contact));
          }
          page.nextPage();
        } while (!response.isEndOfRecords());
        LOG.debug("Get all Datasets owned by Organization finished");
        // return array, required for serialization otherwise get com.sun.jersey.api.MessageException: A message body
        // writer for Java class java.util.ArrayList
        LegacyDatasetResponse[] array = datasets.toArray(new LegacyDatasetResponse[datasets.size()]);
        return Response.status(Response.Status.OK).entity(array).build();
      } catch (NotFoundException e) {
        LOG.error("The organization with key {} specified by query parameter does not exist", organizationKey);
      }
    }
    return Response.status(Response.Status.OK).entity(new ErrorResponse("No organisation matches the key provided"))
      .build();
  }

  /**
   * Read GBRDS Dataset, handling incoming request with path /resource/{key}.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @return Response with Status.OK (200) if dataset exists
   */
  @GET
  @Path("{key}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Consumes(MediaType.TEXT_PLAIN)
  public Response readDataset(@PathParam("key") UUID datasetKey) {
    if (datasetKey != null) {
      try {
        LOG.debug("Get Dataset, key={}", datasetKey);
        // verify Dataset with key exists, otherwise NotFoundException gets thrown
        Dataset dataset = datasetService.get(datasetKey);
        Contact contact = LegacyResourceUtils.getPrimaryContact(dataset);
        return Response.status(Response.Status.OK).entity(new LegacyDatasetResponse(dataset, contact))
          .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
      } catch (NotFoundException e) {
        LOG.error("The dataset with key {} specified by path parameter does not exist", datasetKey);
      }
    }
    return Response.status(Response.Status.OK).entity(new ErrorResponse("No resource matches the key provided"))
      .cacheControl(LegacyResourceConstants.CACHE_CONTROL_DISABLED).build();
  }

  /**
   * Delete GBRDS Dataset, handling incoming request with path /resource/{key}. Only credentials are mandatory.
   * If deletion is successful, returns Response with Status.OK.
   *
   * @param datasetKey dataset key (UUID) coming in as path param
   * @return Response with Status.OK if successful
   * @see IptResource#deleteDataset(java.util.UUID)
   */
  @DELETE
  @Path("{key}")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response deleteDataset(@PathParam("key") UUID datasetKey) {
    // reuse existing method
    return iptResource.deleteDataset(datasetKey);
  }
}
