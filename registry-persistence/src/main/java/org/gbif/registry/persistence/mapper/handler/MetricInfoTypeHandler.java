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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import com.google.common.base.Strings;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

/** Converts a {@link MetricInfo} to a hstore and viceversa. */
public class MetricInfoTypeHandler extends BaseTypeHandler<Set<MetricInfo>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Set<MetricInfo> metrics, JdbcType jdbcType) throws SQLException {

    Map<String, String> metricsAsMap = new HashMap<>();
    if (metrics != null && !metrics.isEmpty()) {
      metricsAsMap =
          metrics.stream().collect(Collectors.toMap(MetricInfo::getName, MetricInfo::getValue));
    }

    ps.setString(i, HStoreConverter.toString(metricsAsMap));
  }

  @Override
  public Set<MetricInfo> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return metricInfoFromString(rs.getString(columnName));
  }

  @Override
  public Set<MetricInfo> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return metricInfoFromString(rs.getString(columnIndex));
  }

  @Override
  public Set<MetricInfo> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return metricInfoFromString(cs.getString(columnIndex));
  }

  private Set<MetricInfo> metricInfoFromString(String hstoreString) {
    if (Strings.isNullOrEmpty(hstoreString)) {
      return new HashSet<>();
    }

    Map<String, String> metricsAsMap = HStoreConverter.fromString(hstoreString);

    return metricsAsMap.entrySet().stream()
        .map(e -> new MetricInfo(e.getKey(), e.getValue()))
        .collect(Collectors.toSet());
  }
}
