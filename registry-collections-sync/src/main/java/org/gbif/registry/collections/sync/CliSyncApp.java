package org.gbif.registry.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.registry.collections.sync.diff.DiffResult;
import org.gbif.registry.collections.sync.diff.IndexHerbariorumDiffFinder;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.ih.IHHttpClient;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.notification.GithubClient;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CliSyncApp {

  public static void main(String[] args) {
    // TODO: if calls to update or create fail put it in a try-catch and follow with the

    log.info("Loading IH");
    // TODO: parametrize
    IHHttpClient ihHttpClient = IHHttpClient.create("http://sweetgum.nybg.org/science/api/v1/");
    CompletableFuture<List<IHInstitution>> ihInstitutionsFuture =
      CompletableFuture.supplyAsync(ihHttpClient::getInstitutions);

    // TODO: parametrize
    GrSciCollHttpClient grSciCollHttpClient =
      GrSciCollHttpClient.create("http://api.gbif-dev.org/v1/grscicoll/", "", "");

    log.info("Loading Institutions");
    CompletableFuture<List<Institution>> institutionsFuture =
      CompletableFuture.supplyAsync(grSciCollHttpClient::getInstitutions);

    log.info("Loading Collections");
    CompletableFuture<List<Collection>> collectionsFuture =
      CompletableFuture.supplyAsync(grSciCollHttpClient::getCollections);

    CompletableFuture.allOf(ihInstitutionsFuture, institutionsFuture, collectionsFuture);

    List<IHInstitution> ihInstitutions = ihInstitutionsFuture.join();
    List<Institution> institutions = institutionsFuture.join();
    List<Collection> collections = collectionsFuture.join();

    GithubClient githubClient = GithubClient.create("", "", ""); // TODO

    DiffResult diffResult =
      IndexHerbariorumDiffFinder.syncIH(
        ihInstitutions, ihHttpClient::getStaffByInstitution, institutions, collections);

    // TODO: dryRun
    diffResult.getInstitutionsToCreate().forEach(grSciCollHttpClient::createInstitution);
    diffResult
      .getInstitutionsToUpdate()
      .forEach(
        r -> {
          grSciCollHttpClient.updateInstitution(r.getNewInstitution());
          handleStaffDiff(grSciCollHttpClient, r.getStaffDiffResult());
        });
    diffResult.getInstitutionConflicts().forEach(githubClient::createIssue);

    diffResult
      .getCollectionsToUpdate()
      .forEach(
        c -> {
          grSciCollHttpClient.updateCollection(c.getNewCollection());
          handleStaffDiff(grSciCollHttpClient, c.getStaffDiffResult());
        });
    diffResult.getCollectionConflicts().forEach(githubClient::createIssue);

    diffResult.getConflicts().forEach(githubClient::createIssue);
  }

  private static void handleStaffDiff(
    GrSciCollHttpClient grSciCollHttpClient, DiffResult.StaffDiffResult s) {
    s.getPersonsToCreate().forEach(grSciCollHttpClient::createPerson);
    s.getPersonsToDelete().forEach(grSciCollHttpClient::deletePerson);
    s.getPersonsToUpdate().forEach(p -> grSciCollHttpClient.updatePerson(p.getNewPerson()));
  }

  private class Config {

    private String registryUser;
    private String registryPassword;
    private String registryWsUrl;
    private String ihWsUrl;
    private boolean dryRun;
    private String githubUser;
    private String githubPassword;
  }
}
