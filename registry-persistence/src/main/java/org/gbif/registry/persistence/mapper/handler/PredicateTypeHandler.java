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

import org.gbif.api.model.predicate.Predicate;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

/** Serializes/deserializes {@link Predicate} objects into/from a JSON string. */
public class PredicateTypeHandler implements TypeHandler<Predicate> {

  private final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Override
  public Predicate getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromJSON(cs.getString(columnIndex));
  }

  @Override
  public Predicate getResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromJSON(rs.getString(columnIndex));
  }

  @Override
  public Predicate getResult(ResultSet rs, String columnName) throws SQLException {
    return fromJSON(rs.getString(columnName));
  }

  @Override
  public void setParameter(PreparedStatement ps, int i, Predicate parameter, JdbcType jdbcType)
      throws SQLException {
    try {
      ps.setObject(
          i, parameter == null ? null : objectMapper.writeValueAsString(parameter), Types.OTHER);
    } catch (Exception e) {
      throw new SQLException(e);
    }
  }

  /** Deserialize a {@link Predicate} object from a JSON string. */
  private Predicate fromJSON(String jsonAsString) throws SQLException {
    try {
      return Strings.isNullOrEmpty(jsonAsString)
          ? null
          : objectMapper.readValue(jsonAsString, Predicate.class);
    } catch (Exception e) {
      throw new SQLException(e);
    }
  }
}
