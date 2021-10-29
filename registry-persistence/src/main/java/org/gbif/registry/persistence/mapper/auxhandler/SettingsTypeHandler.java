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
package org.gbif.registry.persistence.mapper.auxhandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * Provides convertion from the key value pairs to HStore. When reading, the caller is guaranteed
 * ordering of the content.
 *
 * <p>IMPORTANT: This handler is in a separate package because of auto-mapping problems.
 */
public class SettingsTypeHandler extends BaseTypeHandler<Map<String, String>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, HStoreConverter.toString(parameter));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private TreeMap<String, String> fromString(String hstring) {
    TreeMap<String, String> sortedMap = Maps.newTreeMap();
    if (!Strings.isNullOrEmpty(hstring)) {
      sortedMap.putAll(HStoreConverter.fromString(hstring)); // it is indeed <String,String>
    }
    return sortedMap;
  }
}
