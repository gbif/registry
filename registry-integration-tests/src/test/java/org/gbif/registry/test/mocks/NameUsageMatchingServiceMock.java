/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.test.mocks;

import org.gbif.api.vocabulary.Rank;
import org.gbif.kvs.species.NameUsageMatchRequest;
import org.gbif.rest.client.species.Metadata;
import org.gbif.rest.client.species.NameUsageMatchResponse;
import org.gbif.rest.client.species.NameUsageMatchingService;

import java.util.Arrays;

public class NameUsageMatchingServiceMock implements NameUsageMatchingService {

  public static final NameUsageMatchResponse.Usage DEFAULT_USAGE = NameUsageMatchResponse.Usage.builder()
    .withKey("100").withName("usage").withRank(Rank.SPECIES.toString()).build();
  public static final NameUsageMatchResponse.RankedName DEFAULT_HIGHEST_USAGE =
      new NameUsageMatchResponse.RankedName("1", "superHigherUsage", Rank.KINGDOM.toString(), null);


  @Override
  public NameUsageMatchResponse match(NameUsageMatchRequest nameUsageMatchRequest) {
    if ("Aves".equalsIgnoreCase(nameUsageMatchRequest.getScientificName())) {
      return NameUsageMatchResponse.builder()
      .withUsage(NameUsageMatchResponse.Usage.builder().withKey("212").withName("Aves").withRank(Rank.CLASS.toString()).build())
      .withClassification(
              Arrays.asList(
                  new NameUsageMatchResponse.RankedName("1", "Animalia", Rank.KINGDOM.toString(), null),
                  new NameUsageMatchResponse.RankedName("44", "Chordata", Rank.PHYLUM.toString(), null),
                  new NameUsageMatchResponse.RankedName("212", "Aves", Rank.CLASS.toString(), null)))
      .withDiagnostics(NameUsageMatchResponse.Diagnostics.builder().withMatchType(NameUsageMatchResponse.MatchType.EXACT).build())
      .build();
    }

    return NameUsageMatchResponse.builder()
      .withUsage(DEFAULT_USAGE)
      .withClassification(
          Arrays.asList(
              new NameUsageMatchResponse.RankedName("50", "higherUsage", Rank.GENUS.toString(), null),
              DEFAULT_HIGHEST_USAGE
          )
      )
      .withDiagnostics(NameUsageMatchResponse.Diagnostics.builder().withMatchType(NameUsageMatchResponse.MatchType.HIGHERRANK).build())
      .build();
  }

  @Override
  public Metadata getMetadata(String key) {
    return null;
  }
}
