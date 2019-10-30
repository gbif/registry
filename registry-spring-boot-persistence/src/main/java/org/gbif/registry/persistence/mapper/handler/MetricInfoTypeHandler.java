package org.gbif.registry.persistence.mapper.handler;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

/**
 * Converts a {@link MetricInfo} to a hstore and viceversa.
 */
public class MetricInfoTypeHandler extends BaseTypeHandler<Set<MetricInfo>> {

  private static final String METRIC_INFO_DELIMITER = "=>";
  private static final String LIST_DELIMITER = ",";

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
