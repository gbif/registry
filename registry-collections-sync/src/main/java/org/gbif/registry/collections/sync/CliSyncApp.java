package org.gbif.registry.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.api.model.collections.Person;
import org.gbif.registry.collections.sync.diff.*;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.ih.IHHttpClient;
import org.gbif.registry.collections.sync.ih.IHInstitution;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.FailedAction;

@Slf4j
public class CliSyncApp {

  public static void main(String[] args) {
    // parse args
    CliArgs cliArgs = new CliArgs();
    JCommander.newBuilder().addObject(cliArgs).build().parse(args);

    SyncConfig config =
        SyncConfig.fromFileName(cliArgs.confPath)
            .orElseThrow(() -> new IllegalArgumentException("No valid config provided"));

    // load the data from the WS
    log.info("Loading IH");
    IHHttpClient ihHttpClient = IHHttpClient.create(config.getIhWsUrl());
    CompletableFuture<List<IHInstitution>> ihInstitutionsFuture =
        CompletableFuture.supplyAsync(ihHttpClient::getInstitutions);

    GrSciCollHttpClient grSciCollHttpClient = GrSciCollHttpClient.create(config);
    log.info("Loading Institutions");
    CompletableFuture<List<Institution>> institutionsFuture =
        CompletableFuture.supplyAsync(grSciCollHttpClient::getInstitutions);

    log.info("Loading Collections");
    CompletableFuture<List<Collection>> collectionsFuture =
        CompletableFuture.supplyAsync(grSciCollHttpClient::getCollections);

    log.info("Loading Persons");
    CompletableFuture<List<Person>> personsFuture =
        CompletableFuture.supplyAsync(grSciCollHttpClient::getPersons);

    CompletableFuture.allOf(
            ihInstitutionsFuture, institutionsFuture, collectionsFuture, personsFuture)
        .join();

    List<IHInstitution> ihInstitutions = ihInstitutionsFuture.join();
    List<Institution> institutions = institutionsFuture.join();
    List<Collection> collections = collectionsFuture.join();
    List<Person> persons = personsFuture.join();

    // create an entity converter to use in the diff finder process
    EntityConverter entityConverter =
        EntityConverter.builder()
            .countries(ihHttpClient.getCountries())
            .creationUser(config.getRegistryWsUser())
            .build();

    // look for differences
    log.info("Looking for differences");
    DiffResult diffResult =
        IndexHerbariorumDiffFinder.builder()
            .ihInstitutions(ihInstitutions)
            .ihStaffFetcher(ihHttpClient::getStaffByInstitution)
            .institutions(institutions)
            .collections(collections)
            .persons(persons)
            .entityConverter(entityConverter)
            .build()
            .find();

    // handle results
    List<FailedAction> fails =
        DiffResultHandler.builder()
            .diffResult(diffResult)
            .config(config)
            .grSciCollHttpClient(grSciCollHttpClient)
            .build()
            .handle();

    // add fails to result
    log.info("{} operations failed updating the registry", fails.size());
    diffResult.setFailedActions(fails);

    log.info("Diff result: {}", diffResult);

    // save results to a file
    if (config.isSaveResultsToFile()) {
      DiffResultExporter.exportResultsToFile(
          diffResult, Paths.get("ih_sync_result_" + System.currentTimeMillis()));
    }
  }

  private static class CliArgs {
    @Parameter(names = {"--config", "-c"})
    private String confPath;
  }
}
