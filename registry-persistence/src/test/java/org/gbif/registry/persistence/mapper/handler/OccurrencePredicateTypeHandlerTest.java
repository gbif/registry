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
package org.gbif.registry.persistence.mapper.handler;

import org.gbif.api.model.event.search.EventSearchParameter;
import org.gbif.api.model.occurrence.search.OccurrenceSearchParameter;
import org.gbif.api.model.predicate.DisjunctionPredicate;
import org.gbif.api.model.predicate.EqualsPredicate;
import org.gbif.api.model.predicate.Predicate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OccurrencePredicateTypeHandlerTest {

  private final OccurrencePredicateTypeHandler handler = new OccurrencePredicateTypeHandler();

  @Test
  void deserializeOccurrenceSearchParameterKey() throws SQLException {
    String filter =
        """
        {
          "type": "equals",
          "key": "COUNTRY",
          "value": "DK"
        }
        """;

    Predicate predicate = handler.getResult(mockResultSet(filter), "filter");

    assertInstanceOf(EqualsPredicate.class, predicate);
  }

  @Test
  void deserializeEventSearchParameterKeyOnOccurrenceDownload() throws SQLException {
    String filter =
        """
        {
          "type": "or",
          "predicates": [
            {
              "type": "equals",
              "key": "HUMBOLDT_IS_TAXONOMIC_SCOPE_FULLY_REPORTED",
              "value": "true"
            },
            {
              "type": "equals",
              "key": "COUNTRY",
              "value": "DK"
            }
          ]
        }
        """;

    Predicate predicate = handler.getResult(mockResultSet(filter), "filter");

    assertInstanceOf(DisjunctionPredicate.class, predicate);
    DisjunctionPredicate disjunction = (DisjunctionPredicate) predicate;
    assertEquals(2, disjunction.getPredicates().size());

    EqualsPredicate<?> humboldtPredicate =
        (EqualsPredicate<?>) new ArrayList<>(disjunction.getPredicates()).get(0);
    assertInstanceOf(EventSearchParameter.class, humboldtPredicate.getKey());
    assertEquals(
        EventSearchParameter.HUMBOLDT_IS_TAXONOMIC_SCOPE_FULLY_REPORTED,
        humboldtPredicate.getKey());

    EqualsPredicate<?> countryPredicate =
        (EqualsPredicate<?>) new ArrayList<>(disjunction.getPredicates()).get(1);
    assertInstanceOf(OccurrenceSearchParameter.class, countryPredicate.getKey());
    assertEquals(OccurrenceSearchParameter.COUNTRY, countryPredicate.getKey());
  }

  @Test
  void lookupResolvesOccurrenceBeforeEventWhenBothExist() {
    assertEquals(
        OccurrenceSearchParameter.COUNTRY,
        OccurrenceDownloadSearchParameterDeserializers.lookup("COUNTRY").orElseThrow());
  }

  private static ResultSet mockResultSet(String filter) throws SQLException {
    ResultSet resultSet = Mockito.mock(ResultSet.class);
    Mockito.when(resultSet.getString("filter")).thenReturn(filter);
    return resultSet;
  }
}
