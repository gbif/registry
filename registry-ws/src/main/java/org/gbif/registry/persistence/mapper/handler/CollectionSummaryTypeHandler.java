package org.gbif.registry.persistence.mapper.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import static org.gbif.api.model.pipelines.PipelineStep.MetricInfo;

/** Converts a {@link MetricInfo} to a hstore and viceversa. */
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
