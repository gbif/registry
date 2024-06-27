package org.gbif.registry.test.mocks;

import java.util.Arrays;
import org.gbif.api.model.checklistbank.NameUsageMatch;
import org.gbif.api.model.common.LinneanClassification;
import org.gbif.api.v2.NameUsageMatch2;
import org.gbif.api.v2.RankedName;
import org.gbif.api.vocabulary.Rank;
import org.gbif.checklistbank.ws.client.NubResourceClient;

public class NubResourceClientMock implements NubResourceClient {

  public static final RankedName DEFAULT_USAGE = new RankedName(100, "usage", Rank.SPECIES);
  public static final RankedName DEFAULT_HIGHEST_USAGE =
      new RankedName(1, "superHigherUsage", Rank.KINGDOM);

  @Override
  public NameUsageMatch match(
      String s,
      String s1,
      String s2,
      String s3,
      String s4,
      String s5,
      String s6,
      String s7,
      LinneanClassification linneanClassification,
      Boolean aBoolean,
      Boolean aBoolean1) {
    return null;
  }

  @Override
  public NameUsageMatch2 match2(
      String scientificName2,
      String scientificName,
      String authorship2,
      String authorship,
      String rank2,
      String rank,
      String genericName,
      String specificEpithet,
      String infraspecificEpithet,
      LinneanClassification classification,
      Boolean strict,
      Boolean verbose) {
    NameUsageMatch2 nameUsageMatch2 = new NameUsageMatch2();
    nameUsageMatch2.setUsage(DEFAULT_USAGE);

    nameUsageMatch2.setClassification(
        Arrays.asList(new RankedName(50, "higherUsage", Rank.GENUS), DEFAULT_HIGHEST_USAGE));

    return nameUsageMatch2;
  }
}
