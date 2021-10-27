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

import org.gbif.api.vocabulary.collections.CollectionContentType;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.google.common.base.Strings;

/** {@link org.apache.ibatis.type.TypeHandler} for arrays of {@link CollectionContentType}. */
public class CollectionContentTypeArrayTypeHandler
    extends BaseTypeHandler<List<CollectionContentType>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, List<CollectionContentType> parameter, JdbcType jdbcType)
      throws SQLException {
    Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public List<CollectionContentType> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return toList(rs.getArray(columnName));
  }

  @Override
  public List<CollectionContentType> getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return toList(rs.getArray(columnIndex));
  }

  @Override
  public List<CollectionContentType> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return toList(cs.getArray(columnIndex));
  }

  private List<CollectionContentType> toList(Array pgArray) throws SQLException {
    if (pgArray == null) return new ArrayList<>();

    String[] strings = (String[]) pgArray.getArray();
    if (strings != null && strings.length > 0) {
      return Arrays.stream(strings)
          .filter(v -> !Strings.isNullOrEmpty(v))
          .map(CollectionContentType::valueOf)
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }
}
