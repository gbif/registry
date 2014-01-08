package org.gbif.registry.util;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Contact;
import org.gbif.api.model.registry.Contactable;
import org.gbif.api.model.registry.NetworkEntity;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.service.registry.InstallationService;
import org.gbif.api.service.registry.NetworkEntityService;
import org.gbif.api.service.registry.NetworkService;
import org.gbif.api.service.registry.NodeService;
import org.gbif.api.service.registry.OrganizationService;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.ws.client.guice.GbifApplicationAuthModule;

import java.util.Properties;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class does the identity function update for entities in the registry. That is to say that it reads and updates
 * all entities.
 * Because the database was migrated, and we have Java validation rules, this will ensure that all migrated data can be
 * used in updates.
 */
public class IdentityUpdateValidationCheck {

  private static final Logger LOG = LoggerFactory.getLogger(IdentityUpdateValidationCheck.class);

  /**
   * Runs the validation. To run, add the necessary properties as arguments. The key and password are
   * authenticated by the web services, so check they match a key/password combination the web services' know of.
   *
   * @param args Base url
   */
  public static void main(String[] args) {
    Properties p = new Properties();
    p.put("registry.ws.url", args[0]);
    p.setProperty("application.key", args[1]);
    p.setProperty("application.secret", args[2]);
    // Create authentication module, and set principal name, equal to a GBIF User unique account name
    GbifApplicationAuthModule auth = new GbifApplicationAuthModule(p);
    auth.setPrincipal("admin");
    Injector injector = Guice.createInjector(new RegistryWsClientModule(p), auth);

    LOG.info("Starting Node tests");
    int nodeErrorCount = verifyEntity(injector.getInstance(NodeService.class));
    LOG.info("Node tests produced {} errors", nodeErrorCount);

    LOG.info("Starting Organization tests");
    int organisationErrorCount = verifyEntity(injector.getInstance(OrganizationService.class));
    LOG.info("Organization tests produced {} errors", organisationErrorCount);

    LOG.info("Starting Installation tests");
    int installationErrorCount = verifyEntity(injector.getInstance(InstallationService.class));
    LOG.info("Installation tests produced {} errors", installationErrorCount);

    LOG.info("Starting Dataset tests");
    int datasetErrorCount = verifyEntity(injector.getInstance(DatasetService.class));
    LOG.info("Dataset tests produced {} errors", datasetErrorCount);

    LOG.info("Starting Network tests");
    int networkErrorCount = verifyEntity(injector.getInstance(NetworkService.class));
    LOG.info("Network tests produced {} errors", networkErrorCount);

    // summarise again jsut for ease of reading
    LOG.info("Test results:");
    LOG.info("Node tests produced {} errors", nodeErrorCount);
    LOG.info("Organization tests produced {} errors", organisationErrorCount);
    LOG.info("Installation tests produced {} errors", installationErrorCount);
    LOG.info("Dataset tests produced {} errors", datasetErrorCount);
    LOG.info("Network tests produced {} errors", networkErrorCount);

  }

  private static <T extends NetworkEntity> int verifyEntity(NetworkEntityService<T> service) {
    int errorCount = 0;
    PagingRequest page = new PagingRequest(0, 100);
    PagingResponse<T> response = null;
    do {
      response = service.list(page);
      LOG.debug("Page with offset[{}] return[{}] records", page.getOffset(), response.getCount());
      for (T e : response.getResults()) {
        LOG.debug("Updating entity[{}]", e.getKey());
        try {
          service.update(e);

          if (e instanceof Contactable) {
            Contactable contactable = (Contactable) e;
            for (Contact c : contactable.getContacts()) {
              service.updateContact(e.getKey(), c);
            }
          }

        } catch (Exception ex) {
          LOG.error("Unable to update entity[{}]", e.getKey(), ex);
          errorCount++;
        }
      }
      page.nextPage();
    } while (!response.isEndOfRecords());
    return errorCount;
  }
}
