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
package org.gbif.registry.cli.common;

import org.gbif.api.vocabulary.Continent;
import org.gbif.api.vocabulary.GbifRegion;
import org.gbif.api.vocabulary.NodeType;
import org.gbif.api.vocabulary.ParticipationStatus;
import org.gbif.api.vocabulary.directory.ParticipantType;

import com.google.common.collect.ImmutableBiMap;

public class DirectoryRegistryMapping {

  /**
   * Return the Directory ParticipationStatus from a Registry ParticipationStatus
   * PARTICIPATION_STATUS.get(org.gbif.api.vocabulary.directory.ParticipationStatus) returns
   * org.gbif.api.vocabulary.ParticipationStatus
   */
  public static final ImmutableBiMap<
          org.gbif.api.vocabulary.directory.ParticipationStatus, ParticipationStatus>
      PARTICIPATION_STATUS =
          ImmutableBiMap.of(
              org.gbif.api.vocabulary.directory.ParticipationStatus.VOTING,
              ParticipationStatus.VOTING,
              org.gbif.api.vocabulary.directory.ParticipationStatus.OBSERVER,
              ParticipationStatus.OBSERVER,
              org.gbif.api.vocabulary.directory.ParticipationStatus.ASSOCIATE,
              ParticipationStatus.ASSOCIATE,
              org.gbif.api.vocabulary.directory.ParticipationStatus.AFFILIATE,
              ParticipationStatus.AFFILIATE,
              org.gbif.api.vocabulary.directory.ParticipationStatus.FORMER,
              ParticipationStatus.FORMER);

  public static final ImmutableBiMap<ParticipantType, NodeType> PARTICIPATION_TYPE =
      ImmutableBiMap.of(
          ParticipantType.COUNTRY, NodeType.COUNTRY, ParticipantType.OTHER, NodeType.OTHER);

  public static final ImmutableBiMap<GbifRegion, Continent> GBIF_REGION_CONTINENT =
      new ImmutableBiMap.Builder()
          .put(GbifRegion.AFRICA, Continent.AFRICA)
          .put(GbifRegion.ASIA, Continent.ASIA)
          .put(GbifRegion.EUROPE, Continent.EUROPE)
          .put(GbifRegion.LATIN_AMERICA, Continent.SOUTH_AMERICA)
          .put(GbifRegion.NORTH_AMERICA, Continent.NORTH_AMERICA)
          .put(GbifRegion.OCEANIA, Continent.OCEANIA)
          .build();
}
