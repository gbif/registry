/**
 * 
 */
package org.gbif.registry.util;

import org.gbif.api.model.common.paging.PagingRequest;
import org.gbif.api.model.common.paging.PagingResponse;
import org.gbif.api.model.registry.Dataset;
import org.gbif.api.service.registry.DatasetService;
import org.gbif.api.vocabulary.IdentifierType;
import org.gbif.registry.ws.client.guice.RegistryWsClientModule;
import org.gbif.ws.client.guice.AnonymousAuthModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.regex.Pattern;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A quick hack to create a test file for inspecting dataset keys the migration.
 * Can be deleted once registry 2 goes live. Expects to run on an input file created with:
 * mysql> select id, gbif_registry_uuid, name, occurrence_count, data_provider_id from data_resource where
 * occurrence_count>0 and deleted
 * is null into outfile '/tmp/live_portal.txt';
 * Query OK, 14744 rows affected (0.14 sec)
 */
public class PortalMigrationLookup {

  private static final Logger LOG = LoggerFactory.getLogger(PortalMigrationLookup.class);

  public static void main(String[] args) throws Exception {
    Properties p = new Properties();
    p.put("registry.ws.url", args[0]);
    Injector injector = Guice.createInjector(new RegistryWsClientModule(p), new AnonymousAuthModule());

    LOG.info("Starting test");
    DatasetService service = injector.getInstance(DatasetService.class);

    BufferedReader reader = new BufferedReader(new FileReader("/tmp/live_portal.txt"));
    String line = reader.readLine();
    Pattern tab = Pattern.compile("\t");
    int goodCount = 0;
    int missingCount = 0;
    int ambigiousCount = 0;
    int problemOccurrences = 0;

    BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/problemDatasets.txt"));
    writer.write("dataResourceId\tdataProviderId\toccurrenceCount\tname\n");

    while (line != null) {
      String atoms[] = tab.split(line);

      PagingResponse<Dataset> results =
        service.listByIdentifier(IdentifierType.GBIF_PORTAL, atoms[0], new PagingRequest());

      // sorry to anyone looking at this - hacky as hell...
      String id = atoms[0];
      String name = atoms[2];
      int occurrenceCount = Integer.parseInt(atoms[3]);
      String provider_id = atoms[4];

      if (results.getCount() == 1) {
        LOG.info("Good {}: {}", id, results.getResults().get(0).getKey());
        goodCount++;
      } else if (results.getCount() > 1) {
        LOG.info("BAD  {} has {} options", atoms[0], results.getCount());
        ambigiousCount++;
        problemOccurrences += occurrenceCount;
      } else {
        LOG.info("BAD  {} has no options", atoms[0]);
        missingCount++;
        problemOccurrences += occurrenceCount;

        writer.write(id);
        writer.write("\t");
        writer.write(provider_id);
        writer.write("\t");
        writer.write("" + occurrenceCount);
        writer.write("\t");
        writer.write(name);
        writer.write("\n");
      }

      line = reader.readLine();
    }
    writer.close();

    LOG.info("Good[{}] missing[{}] ambigious[{}] problemOccurrences[{}]", goodCount, missingCount, ambigiousCount,
      problemOccurrences);
  }
}
