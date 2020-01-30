package org.gbif.registry.ws.resources;

import org.gbif.registry.DatabaseInitializer;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
  strict = true,
  features = {
    "classpath:features/occurrence_download.feature",
    "classpath:features/occurrence_download_statistic.feature",
    "classpath:features/occurrence_download_usage.feature"
  },
  glue = {
    "org.gbif.registry.ws.resources.occurrencedownload",
    "org.gbif.registry.utils.cucumber"
  },
  plugin = "pretty"
)
public class OccurrenceDownloadIT {

  @ClassRule
  public static DatabaseInitializer databaseInitializer = new DatabaseInitializer();

}
