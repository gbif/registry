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

import org.gbif.api.model.pipelines.StepType;
import org.gbif.api.vocabulary.collections.PreservationType;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.google.common.base.Strings;

/** {@link org.apache.ibatis.type.TypeHandler} for arrays of {@link PreservationType}. */
public class StepTypeArrayTypeHandler extends BaseTypeHandler<Set<StepType>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Set<StepType> parameter, JdbcType jdbcType)
      throws SQLException {
    Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public Set<StepType> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toSet(rs.getArray(columnName));
  }

  @Override
  public Set<StepType> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toSet(rs.getArray(columnIndex));
  }

  @Override
  public Set<StepType> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return toSet(cs.getArray(columnIndex));
  }

  private Set<StepType> toSet(Array pgArray) throws SQLException {
    if (pgArray == null) {
      return new HashSet<>();
    }

    String[] strings = (String[]) pgArray.getArray();
    if (strings != null && strings.length > 0) {
      return Arrays.stream(strings)
          .filter(v -> !Strings.isNullOrEmpty(v))
          .map(StepType::valueOf)
          .collect(Collectors.toSet());
    }
    return new HashSet<>();
  }
}
