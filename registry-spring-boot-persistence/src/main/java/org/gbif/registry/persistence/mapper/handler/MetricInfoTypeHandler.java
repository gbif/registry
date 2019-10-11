package org.gbif.registry.persistence.mapper.handler;

import com.google.common.base.Strings;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.UnaryOperator;
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

    String metricsAsString = null;
    if (metrics != null && !metrics.isEmpty()) {
      metricsAsString =
          metrics.stream()
              .map(
                  metricInfo ->
                      new StringJoiner(METRIC_INFO_DELIMITER)
                          .add(metricInfo.getName())
                          .add(Optional.ofNullable(metricInfo.getValue())
                              .filter(s -> !Strings.isNullOrEmpty(s))
                              .orElse(null))
                          .toString())
              .collect(Collectors.joining(LIST_DELIMITER));
    }

    ps.setString(i, metricsAsString);
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

    // removes the quotes at the beginning and at the end if they exist
    UnaryOperator<String> stringNormalizer =
        s ->
            Optional.of(
                s.substring(
                    s.charAt(0) == '"' ? 1 : 0,
                    s.charAt(s.length() - 1) == '"' ? s.length() - 1 : s.length()))
                .filter(v -> !v.equalsIgnoreCase("null"))
                .orElse(null);

    return Arrays.stream(hstoreString.split(LIST_DELIMITER))
        .map(s -> s.split(METRIC_INFO_DELIMITER))
        .map(
            pieces ->
                new MetricInfo(
                    stringNormalizer.apply(pieces[0]), stringNormalizer.apply(pieces[1])))
        .collect(Collectors.toSet());
  }
}
