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

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedJdbcTypes(JdbcType.ARRAY)
@MappedTypes(Set.class)
public class SetArrayTypeHandler extends BaseTypeHandler<Set<String>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType)
      throws SQLException {
    Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return convertArrayToSet(rs.getArray(columnName));
  }

  @Override
  public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return convertArrayToSet(rs.getArray(columnIndex));
  }

  @Override
  public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return convertArrayToSet(cs.getArray(columnIndex));
  }

  private Set<String> convertArrayToSet(Array array) throws SQLException {
    if (array == null) {
      return null;
    }
    Object[] objArray = (Object[]) array.getArray();
    Set<String> result = new HashSet<>();
    for (Object obj : objArray) {
      if (obj != null) {
        result.add(obj.toString());
      }
    }
    return result;
  }
}
