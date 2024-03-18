package org.gbif.registry.search;

import org.gbif.api.vocabulary.Country;
import org.gbif.api.vocabulary.DatasetType;
import org.gbif.api.vocabulary.Habitat;
import org.gbif.api.vocabulary.License;
import org.gbif.api.vocabulary.NameType;
import org.gbif.api.vocabulary.NomenclaturalStatus;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumSet;

public class NLQueriesGen {


  public static void main(String[] args) throws IOException {

    try(BufferedWriter writer = new BufferedWriter(new FileWriter("datasets_nl_queries.txt"))) {
      for (DatasetType datasetType : EnumSet.of(DatasetType.OCCURRENCE, DatasetType.CHECKLIST, DatasetType.METADATA)) {
        for (Country country : EnumSet.of(Country.SPAIN, Country.COSTA_RICA, Country.UNITED_STATES, Country.DENMARK, Country.AUSTRALIA)) {
          writer.write(String.format("list GBIF datasets of publishingCountry %s and type %s.", country.getIso2LetterCode(), datasetType));
          writer.newLine();
          writer.write(String.format("list GBIF datasets of hostingCountry %s and type %s.", country.getIso2LetterCode(), datasetType));
          writer.newLine();
          for (License license : EnumSet.of(License.CC_BY_4_0, License.CC_BY_NC_4_0)) {
            writer.write(String.format("list GBIF datasets of publishingCountry %s and type %s and license %s.", country.getIso2LetterCode(), datasetType, license));
            writer.newLine();
          }
        }
      }
    }


    try(BufferedWriter writer = new BufferedWriter(new FileWriter("species_nl_queries.txt"))) {
      for (NameType nameType : EnumSet.of(NameType.SCIENTIFIC, NameType.OTU)) {
        writer.write(String.format("list GBIF species with nameType %s.", nameType));
        writer.newLine();
        for (Habitat habitat : EnumSet.allOf(Habitat.class)) {
          writer.write(String.format("list GBIF species with nameType %s in habitat %s.", nameType, habitat));
          writer.newLine();
        }
      }
    }
  }
}
