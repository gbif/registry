package org.gbif.registry.collections.sync;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.Institution;
import org.gbif.registry.collections.sync.diff.DiffResult;
import org.gbif.registry.collections.sync.diff.DiffResult.PersonDiffResult;
import org.gbif.registry.collections.sync.diff.IndexHerbariorumDiffFinder;
import org.gbif.registry.collections.sync.grscicoll.GrSciCollHttpClient;
import org.gbif.registry.collections.sync.ih.IHHttpClient;
import org.gbif.registry.collections.sync.ih.IHInstitution;
import org.gbif.registry.collections.sync.notification.GithubClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.InstitutionDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;

@Slf4j
public class CliSyncApp {

  public static void main(String[] args) throws IOException {
    SyncConfig config =
        SyncConfig.getConfig(args)
            .orElseThrow(() -> new IllegalArgumentException("Couldn't load the config"));

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

    CompletableFuture.allOf(ihInstitutionsFuture, institutionsFuture, collectionsFuture);

    List<IHInstitution> ihInstitutions = ihInstitutionsFuture.join();
    List<Institution> institutions = institutionsFuture.join();
    List<Collection> collections = collectionsFuture.join();

    DiffResult diffResult =
        IndexHerbariorumDiffFinder.syncIH(
            ihInstitutions, ihHttpClient::getStaffByInstitution, institutions, collections);

    log.info("Diff result: {}", diffResult);

    if (config.isSaveResultsToFile()) {
      ResultsSaver.saveResults(diffResult);
    }

    if (config.isDryRun()) {
      log.info("Skipping updates.. dry run is enabled.");
      return;
    }

    GithubClient githubClient = GithubClient.create(config);

    diffResult.getInstitutionsToCreate().forEach(grSciCollHttpClient::createInstitution);
    diffResult
        .getInstitutionsToUpdate()
        .forEach(
            r -> {
              executeSilently(() -> grSciCollHttpClient.updateInstitution(r.getNewInstitution()));
              handleStaffDiff(grSciCollHttpClient, r.getStaffDiffResult());

              if (!config.isIgnoreConflicts()) {
                r.getStaffDiffResult()
                    .getConflicts()
                    .forEach(i -> executeSilently(() -> githubClient.createIssue(i)));
              }
            });

    diffResult
        .getCollectionsToUpdate()
        .forEach(
            c -> {
              grSciCollHttpClient.updateCollection(c.getNewCollection());
              handleStaffDiff(grSciCollHttpClient, c.getStaffDiffResult());

              if (!config.isIgnoreConflicts()) {
                c.getStaffDiffResult()
                    .getConflicts()
                    .forEach(i -> executeSilently(() -> githubClient.createIssue(i)));
              }
            });

    if (!config.isIgnoreConflicts()) {
      diffResult
          .getInstitutionConflicts()
          .forEach(i -> executeSilently(() -> githubClient.createIssue(i)));
      diffResult
          .getCollectionConflicts()
          .forEach(i -> executeSilently(() -> githubClient.createIssue(i)));
      diffResult.getConflicts().forEach(i -> executeSilently(() -> githubClient.createIssue(i)));
    }
  }

  private static void handleStaffDiff(GrSciCollHttpClient grSciCollHttpClient, StaffDiffResult s) {
    s.getPersonsToCreate().forEach(p -> executeSilently(() -> grSciCollHttpClient.createPerson(p)));
    s.getPersonsToDelete().forEach(p -> executeSilently(() -> grSciCollHttpClient.deletePerson(p)));
    s.getPersonsToUpdate()
        .forEach(p -> executeSilently(() -> grSciCollHttpClient.updatePerson(p.getNewPerson())));
  }

  private static void executeSilently(Runnable runnable) {
    try {
      runnable.run();
    } catch (Exception ex) {
      log.error("Error updating GrSciColl from IH sync", ex);
    }
  }

  private static class ResultsSaver {
    private static final String SECTION_SEPARATOR =
        "##########################################################################";

    private static final String SUBSECTION_SEPARATOR =
        "--------------------------------------------------------------------------";

    static void saveResults(DiffResult diffResult) {

      try (BufferedWriter writer =
          Files.newBufferedWriter(Paths.get("ih_sync_result_" + System.currentTimeMillis()))) {

        printWithNewLineAfter(writer, "IH Sync " + LocalDateTime.now());

        // Institutions
        printSection(writer, "Institutions No Change", diffResult.getInstitutionsNoChange());
        printSection(writer, "Institutions to Create", diffResult.getInstitutionsToCreate());
        printSectionTitle(writer, "Institutions to Update");
        for (InstitutionDiffResult diff : diffResult.getInstitutionsToUpdate()) {
          printWithNewLineAfter(writer, "OLD: " + diff.getOldInstitution());
          printWithNewLineAfter(writer, "NEW: " + diff.getNewInstitution());
          printStaffDiffResult(writer, diff.getStaffDiffResult());
        }
        printSection(writer, "Institution Conflicts", diffResult.getInstitutionConflicts());

        // Collections
        printSection(writer, "Collections No Change", diffResult.getCollectionsNoChange());
        printSectionTitle(writer, "Collections to Update");
        for (DiffResult.CollectionDiffResult diff : diffResult.getCollectionsToUpdate()) {
          printWithNewLineAfter(writer, "OLD: " + diff.getOldCollection());
          printWithNewLineAfter(writer, "NEW: " + diff.getNewCollection());
          printStaffDiffResult(writer, diff.getStaffDiffResult());
        }
        printSection(writer, "Collection Conflicts", diffResult.getCollectionConflicts());

        // Conflicts
        printSection(writer, "General Conflicts", diffResult.getConflicts());

      } catch (Exception e) {
        log.warn("Couldn't save diff results", e);
      }
    }

    private static void printSectionTitle(BufferedWriter writer, String title) throws IOException {
      writer.write(title);
      writer.newLine();
      writer.write(SECTION_SEPARATOR);
      writer.newLine();
    }

    private static void printSubsectionTitle(BufferedWriter writer, String title)
        throws IOException {
      writer.write(title);
      writer.newLine();
      writer.write(SUBSECTION_SEPARATOR);
      writer.newLine();
    }

    private static <T> void printSection(BufferedWriter writer, String title, List<T> collection)
        throws IOException {
      printSectionTitle(writer, title);
      printCollection(writer, collection);
      writer.newLine();
    }

    private static <T> void printSubsection(BufferedWriter writer, String title, List<T> collection)
        throws IOException {
      printSubsectionTitle(writer, title);
      printCollection(writer, collection);
      writer.newLine();
    }

    private static void printStaffDiffResult(BufferedWriter writer, StaffDiffResult staffDiffResult)
        throws IOException {
      printWithNewLineAfter(writer, ">>>> Staff");

      printSubsection(writer, "Staff No Change", staffDiffResult.getPersonsNoChange());
      printSubsection(writer, "Staff to Create", staffDiffResult.getPersonsToCreate());
      printSubsection(writer, "Staff to Delete", staffDiffResult.getPersonsToDelete());

      printSubsectionTitle(writer, "Staff to Update");
      for (PersonDiffResult staffUpdate : staffDiffResult.getPersonsToUpdate()) {
        printWithNewLineAfter(writer, "OLD: " + staffUpdate.getOldPerson());
        printWithNewLineAfter(writer, "NEW: " + staffUpdate.getNewPerson());
      }

      printSubsection(writer, "Staff Conflicts", staffDiffResult.getConflicts());
    }

    private static <T> void printCollection(BufferedWriter writer, List<T> collection)
        throws IOException {
      for (T e : collection) {
        printWithNewLineAfter(writer, e.toString());
      }
    }

    private static void printWithNewLineAfter(BufferedWriter writer, String text)
        throws IOException {
      writer.write(text);
      writer.newLine();
    }
  }
}
