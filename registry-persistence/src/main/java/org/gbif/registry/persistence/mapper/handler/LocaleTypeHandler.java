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
import java.sql.Types;
import java.util.Locale;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

/**
 * A converter for DOI objects to the database representation using the DOI name. Nulls are passed
 * through.
 */
@MappedTypes({Locale.class})
public class LocaleTypeHandler implements TypeHandler<Locale> {

  @Override
  public void setParameter(PreparedStatement ps, int i, Locale parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setObject(i, parameter == null ? null : parameter.toLanguageTag(), Types.OTHER);
  }

  @Override
  public Locale getResult(ResultSet rs, int columnIndex) throws SQLException {
    String column = rs.getString(columnIndex);
    return column != null ? Locale.forLanguageTag(column.trim()) : null;
  }

  @Override
  public Locale getResult(ResultSet rs, String columnName) throws SQLException {
    String column = rs.getString(columnName);
    return column != null ? Locale.forLanguageTag(column.trim()) : null;
  }

  @Override
  public Locale getResult(CallableStatement cs, int columnIndex) throws SQLException {
    String column = cs.getString(columnIndex);
    return column != null ? Locale.forLanguageTag(column.trim()) : null;
  }
}
