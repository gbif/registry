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

import org.gbif.api.v2.RankedName;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Strings;

public class RankedNameListTypeHandler extends BaseTypeHandler<List<RankedName>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final ObjectReader OBJECT_READER =
      OBJECT_MAPPER.readerFor(new TypeReference<List<RankedName>>() {});

  @Override
  public void setNonNullParameter(
      PreparedStatement preparedStatement,
      int i,
      List<RankedName> rankedNamesList,
      JdbcType jdbcType)
      throws SQLException {
    preparedStatement.setString(i, toString(rankedNamesList));
  }

  @Override
  public List<RankedName> getNullableResult(ResultSet resultSet, String columnName)
      throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public List<RankedName> getNullableResult(ResultSet resultSet, int columnIndex)
      throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public List<RankedName> getNullableResult(CallableStatement callableStatement, int columnIndex)
      throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private String toString(List<RankedName> rankedNamesList) {
    try {
      return OBJECT_MAPPER.writeValueAsString(rankedNamesList);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Couldn't convert language map to JSON: " + rankedNamesList.toString(), e);
    }
  }

  private List<RankedName> fromString(String json) {
    if (Strings.isNullOrEmpty(json)) {
      return Collections.emptyList();
    }

    try {
      return OBJECT_READER.readValue(json);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Couldn't deserialize taxon classification JSON from DB: " + json, e);
    }
  }
}
