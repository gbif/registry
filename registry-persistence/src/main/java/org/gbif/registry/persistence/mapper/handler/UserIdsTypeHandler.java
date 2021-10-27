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

import org.gbif.api.model.collections.IdType;
import org.gbif.api.model.collections.UserId;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

/** {@link org.apache.ibatis.type.TypeHandler} for {@link UserId} stored as jsonb. */
public class UserIdsTypeHandler extends BaseTypeHandler<List<UserId>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, List<UserId> parameter, JdbcType jdbcType) throws SQLException {
    ps.setString(i, toString(parameter));
  }

  @Override
  public List<UserId> getNullableResult(ResultSet resultSet, String columnName)
      throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public List<UserId> getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public List<UserId> getNullableResult(CallableStatement callableStatement, int columnIndex)
      throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private String toString(List<UserId> ids) {
    try {
      return OBJECT_MAPPER.writeValueAsString(ids);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Couldn't convert user ids to JSON: " + ids.toString(), e);
    }
  }

  private List<UserId> fromString(String json) {
    if (Strings.isNullOrEmpty(json)) {
      return new ArrayList<>();
    }

    try {
      JsonNode root = OBJECT_MAPPER.readTree(json);
      List<UserId> ids = new ArrayList<>();
      for (JsonNode element : root) {
        UserId userId = new UserId();
        userId.setType(IdType.valueOf(element.get("type").asText()));
        userId.setId(element.get("id").asText());
        ids.add(userId);
      }

      return ids;
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't deserialize JSON from DB: " + json, e);
    }
  }
}
