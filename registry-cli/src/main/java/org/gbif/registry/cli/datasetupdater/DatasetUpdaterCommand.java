package org.gbif.registry.cli.datasetupdater;

import org.gbif.cli.BaseCommand;
import org.gbif.cli.Command;
import org.gbif.registry.cli.common.SingleColumnFileReader;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

import com.beust.jcommander.internal.Lists;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command update either a single dataset or a list of datasets by reinterpreting their preferred metadata
 * document stored in the registry.
 */
@MetaInfServices(Command.class)
public class DatasetUpdaterCommand extends BaseCommand {

  private static final Logger LOG = LoggerFactory.getLogger(DatasetUpdaterCommand.class);
  private final DatasetUpdaterConfiguration config;

  public DatasetUpdaterCommand() {
    super("dataset-updater");
    config = new DatasetUpdaterConfiguration();
  }

  @VisibleForTesting
  public DatasetUpdaterCommand(DatasetUpdaterConfiguration config) {
    super("dataset-updater");
    this.config = config;
  }

  @Override
  protected Object getConfigurationObject() {
    return config;
  }

  /**
   * Updates single dataset from dataset key parameter if detected.
   * </br>
   * Otherwise, updates list of datasets from keyFilePath parameter if detected.
   * </br>
   * In case neither parameters are detected, method exists.
   */
  @Override
  protected void doRun() {
    if (config.key == null && config.keyFilePath == null) {
      LOG.error("Both key and keyFilePath are null - one of them has to be set. Exiting.");
      return;
    }

    DatasetUpdater updater = DatasetUpdater.build(config);
    if (config.key != null) {
      updater.update(UUID.fromString(config.key));
      LOG.info("{} out of 1 datasets were updated", updater.getUpdateCounter());
    } else {
      List<UUID> keys = readKeys(config.keyFilePath);
      updater.update(keys);
      LOG.info("{} out of {} datasets were updated", updater.getUpdateCounter(), keys.size());
    }
  }

  /**
   * Read the single column file of Dataset keys and return list of UUID keys. Method skips and logs strings that
   * are invalid UUIDs.
   *
   * @param keyFilePath the path of file to read
   *
   * @return list of UUIDs read from file
   */
  @VisibleForTesting
  protected List<UUID> readKeys(String keyFilePath) {
    List<UUID> keys = Lists.newArrayList();
    try {
      keys = SingleColumnFileReader.readFile(keyFilePath, new Function<String, UUID>() {
        @Nullable
        @Override
        public UUID apply(String input) {
          try {
            return UUID.fromString(input);
          } catch (IllegalArgumentException e) {
            LOG.error("Key {} is an invalid UUID - skipping!", input);
          }
          return null;
        }
      });
    } catch (IOException e) {
      LOG.error("Error while reading csv key file [{}]. Exiting", keyFilePath, e);
    }
    return keys;
  }
}
