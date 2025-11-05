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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * MyBatis TypeHandler to convert between PostgreSQL TIMESTAMPTZ and Java LocalDateTime.
 * This handler converts TIMESTAMPTZ to LocalDateTime by using the system's default timezone.
 * Truncates to microseconds to match PostgreSQL timestamp precision.
 */
@MappedTypes(LocalDateTime.class)
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
      throws SQLException {
    // Truncate to microseconds to match PostgreSQL precision
    LocalDateTime truncated = parameter.truncatedTo(ChronoUnit.MICROS);
    ps.setTimestamp(i, Timestamp.valueOf(truncated));
  }

  @Override
  public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnName);
    return getLocalDateTime(timestamp);
  }

  @Override
  public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(columnIndex);
    return getLocalDateTime(timestamp);
  }

  @Override
  public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    Timestamp timestamp = cs.getTimestamp(columnIndex);
    return getLocalDateTime(timestamp);
  }

  private static LocalDateTime getLocalDateTime(Timestamp timestamp) {
    if (timestamp != null) {
      LocalDateTime localDateTime = timestamp.toLocalDateTime();
      // Truncate to microseconds to match PostgreSQL precision
      return localDateTime.truncatedTo(ChronoUnit.MICROS);
    }
    return null;
  }
}
