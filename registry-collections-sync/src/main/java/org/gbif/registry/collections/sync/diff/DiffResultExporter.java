package org.gbif.registry.collections.sync.diff;

import org.gbif.api.model.collections.Collection;
import org.gbif.api.model.collections.CollectionEntity;
import org.gbif.api.model.collections.Institution;
import org.gbif.registry.collections.sync.diff.DiffResult.EntityDiffResult;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.registry.collections.sync.diff.DiffResult.PersonDiffResult;
import static org.gbif.registry.collections.sync.diff.DiffResult.StaffDiffResult;

/** Exports a {@link DiffResult} to a file. */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DiffResultExporter {

  private static final String SECTION_SEPARATOR =
      "##########################################################################";
  private static final String SUBSECTION_SEPARATOR =
      "--------------------------------------------------------------------------";
  private static final String LINE_STARTER = ">";
  private static final String SIMPLE_INDENT = "\t";
  private static final String DOUBLE_INDENT = "\t\t";

  public static void exportResultsToFile(DiffResult diffResult, Path filePath) {

    try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {

      printWithNewLineAfter(writer, "IH Sync " + LocalDateTime.now());
      printWithNewLineAfter(writer, "Summary:");
      printWithNewLineAfter(writer, SUBSECTION_SEPARATOR);
      printWithNewLineAfter(
          writer, "Institutions No Change: " + diffResult.getInstitutionsNoChange().size());
      printWithNewLineAfter(
          writer, "Institutions To Create: " + diffResult.getInstitutionsToCreate().size());
      printWithNewLineAfter(
          writer,
          "Institutions To Update: "
              + diffResult.getInstitutionsToUpdate().size()
              + printOnlyStaffUpdates(diffResult.getInstitutionsToUpdate()));
      printWithNewLineAfter(
          writer, "Collections No Change: " + diffResult.getCollectionsNoChange().size());
      printWithNewLineAfter(
          writer,
          "Collections To Update: "
              + diffResult.getCollectionsToUpdate().size()
              + printOnlyStaffUpdates(diffResult.getCollectionsToUpdate()));
      printWithNewLineAfter(
          writer, "Outdated institutions: " + diffResult.getOutdatedIHInstitutions().size());
      printWithNewLineAfter(writer, "General Conflicts: " + diffResult.getConflicts().size());
      printWithNewLineAfter(
          writer,
          "Failed Actions (updates or conflict notifications that failed): "
              + diffResult.getFailedActions().size());
      writer.newLine();
      writer.newLine();

      // Institutions
      printSection(writer, "Institutions No Change", diffResult.getInstitutionsNoChange());
      printSection(writer, "Institutions to Create", diffResult.getInstitutionsToCreate());
      printSectionTitle(
          writer,
          "Institutions to Update: "
              + diffResult.getInstitutionsToUpdate().size()
              + printOnlyStaffUpdates(diffResult.getInstitutionsToUpdate()));
      for (EntityDiffResult<Institution> diff : diffResult.getInstitutionsToUpdate()) {
        writer.write(LINE_STARTER);
        printWithNewLineAfter(writer, "UPDATE DIFF:");
        printWithNewLineAfter(writer, SIMPLE_INDENT + "OLD: " + diff.getOldEntity());
        printWithNewLineAfter(writer, SIMPLE_INDENT + "NEW: " + diff.getNewEntity());
        printStaffDiffResult(writer, diff.getStaffDiffResult());
      }

      // Collections
      printSection(writer, "Collections No Change", diffResult.getCollectionsNoChange());
      printSectionTitle(
          writer,
          "Collections to Update: "
              + diffResult.getCollectionsToUpdate().size()
              + printOnlyStaffUpdates(diffResult.getInstitutionsToUpdate()));
      for (EntityDiffResult<Collection> diff : diffResult.getCollectionsToUpdate()) {
        writer.write(LINE_STARTER);
        printWithNewLineAfter(writer, "UPDATE DIFF:");
        printWithNewLineAfter(writer, SIMPLE_INDENT + "OLD: " + diff.getOldEntity());
        printWithNewLineAfter(writer, SIMPLE_INDENT + "NEW: " + diff.getNewEntity());
        printStaffDiffResult(writer, diff.getStaffDiffResult());
      }

      // Outdated
      printSection(writer, "Outdated Institutions", diffResult.getOutdatedIHInstitutions());

      // Conflicts
      printSection(writer, "General Conflicts", diffResult.getConflicts());

      // fails
      printSection(writer, "Failed Actions", diffResult.getFailedActions());

    } catch (Exception e) {
      log.warn("Couldn't save diff results", e);
    }
  }

  private static <T extends CollectionEntity> String printOnlyStaffUpdates(
      List<EntityDiffResult<T>> entityDiffResult) {
    long onlyStaffUpdate =
        entityDiffResult.stream()
            .filter(d -> d.getNewEntity() == null && d.getOldEntity() == null)
            .count();

    return " ( " + onlyStaffUpdate + " only Staff update)";
  }

  private static void printSectionTitle(BufferedWriter writer, String title) throws IOException {
    writer.write(title);
    writer.newLine();
    writer.write(SECTION_SEPARATOR);
    writer.newLine();
    writer.write(LINE_STARTER);
  }

  private static void printSubsectionTitle(BufferedWriter writer, String title) throws IOException {
    writer.write(DOUBLE_INDENT + title);
    writer.newLine();
    writer.write(DOUBLE_INDENT + SUBSECTION_SEPARATOR);
    writer.newLine();
    writer.write(DOUBLE_INDENT + LINE_STARTER);
  }

  private static <T> void printSection(BufferedWriter writer, String title, List<T> collection)
      throws IOException {
    writer.newLine();
    printSectionTitle(writer, title + ": " + collection.size());
    printCollection(writer, collection);
    writer.newLine();
  }

  private static <T> void printSubsection(BufferedWriter writer, String title, List<T> collection)
      throws IOException {
    writer.newLine();
    printSubsectionTitle(writer, title + ": " + collection.size());
    printCollectionSubsection(writer, collection);
    writer.newLine();
  }

  private static <T extends CollectionEntity> void printStaffDiffResult(
      BufferedWriter writer, StaffDiffResult<T> staffDiffResult) throws IOException {
    printWithNewLineAfter(writer, ">>> Differences in Associated Staff");

    printSubsection(writer, "Staff No Change", staffDiffResult.getPersonsNoChange());
    printSubsection(
        writer, "Staff to Create and add to the entity", staffDiffResult.getPersonsToCreate());
    printSubsection(
        writer, "Staff to Remove from entity", staffDiffResult.getPersonsToRemoveFromEntity());

    printSubsectionTitle(writer, "Staff to Update: " + staffDiffResult.getPersonsToUpdate().size());
    for (PersonDiffResult staffUpdate : staffDiffResult.getPersonsToUpdate()) {
      writer.write(LINE_STARTER);
      printWithNewLineAfter(writer, DOUBLE_INDENT + "STAFF DIFF:");
      printWithNewLineAfter(writer, DOUBLE_INDENT + "OLD: " + staffUpdate.getOldPerson());
      printWithNewLineAfter(writer, DOUBLE_INDENT + "NEW: " + staffUpdate.getNewPerson());
    }

    printSubsection(writer, "Outdated Staff", staffDiffResult.getOutdatedStaff());
    printSubsection(writer, "Staff Conflicts", staffDiffResult.getConflicts());
  }

  private static <T> void printCollection(BufferedWriter writer, List<T> collection)
      throws IOException {
    for (T e : collection) {
      writer.write(LINE_STARTER);
      printWithNewLineAfter(writer, e.toString());
    }
  }

  private static <T> void printCollectionSubsection(BufferedWriter writer, List<T> collection)
      throws IOException {
    for (T e : collection) {
      writer.write(DOUBLE_INDENT + LINE_STARTER);
      printWithNewLineAfter(writer, DOUBLE_INDENT + e.toString());
    }
  }

  private static void printWithNewLineAfter(BufferedWriter writer, String text) throws IOException {
    writer.write(text);
    writer.newLine();
  }
}
