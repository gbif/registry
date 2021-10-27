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
package org.gbif.registry.persistence.mapper.auxhandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import com.google.common.base.Strings;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

/**
 * Converts a {@link MetricInfo} to a hstore and viceversa.
 *
 * <p>IMPORTANT: This handler is in a separate package because of auto-mapping problems.
 */
public class CollectionSummaryTypeHandler extends BaseTypeHandler<Map<String, Integer>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Map<String, Integer> collectionSummary, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, HStoreConverter.toString(collectionSummary));
  }

  @Override
  public Map<String, Integer> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return metricInfoFromString(rs.getString(columnName));
  }

  @Override
  public Map<String, Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return metricInfoFromString(rs.getString(columnIndex));
  }

  @Override
  public Map<String, Integer> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return metricInfoFromString(cs.getString(columnIndex));
  }

  private Map<String, Integer> metricInfoFromString(String hstoreString) {
    if (Strings.isNullOrEmpty(hstoreString)) {
      return new HashMap<>();
    }

    Map<String, String> collectionSummaryMap = HStoreConverter.fromString(hstoreString);

    return collectionSummaryMap.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Integer.valueOf(e.getValue())));
  }
}
