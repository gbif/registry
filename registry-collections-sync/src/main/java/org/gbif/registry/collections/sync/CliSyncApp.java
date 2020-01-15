package org.gbif.registry.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.registry.collections.sync.diff.DiffResult;
import org.gbif.registry.collections.sync.diff.DiffResultExporter;
import org.gbif.registry.collections.sync.diff.DiffResultHandler;
import org.gbif.registry.collections.sync.diff.IndexHerbariorumDiffFinder;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.ih.IHHttpClient;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.notification.GithubClient;

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

    CompletableFuture.allOf(ihInstitutionsFuture, institutionsFuture, collectionsFuture).join();

    List<IHInstitution> ihInstitutions = ihInstitutionsFuture.join();
    List<Institution> institutions = institutionsFuture.join();
    List<Collection> collections = collectionsFuture.join();

    // look for differences
    DiffResult diffResult =
        IndexHerbariorumDiffFinder.syncIH(
            ihInstitutions, ihHttpClient::getStaffByInstitution, institutions, collections);

    // handle results
    List<FailedAction> fails =
        DiffResultHandler.builder()
            .diffResult(diffResult)
            .config(config)
            .grSciCollHttpClient(grSciCollHttpClient)
            .githubClient(GithubClient.create(config))
            .build()
            .handle();

    // add fails to result
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
