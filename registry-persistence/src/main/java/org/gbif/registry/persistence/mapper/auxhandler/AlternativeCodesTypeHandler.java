/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import org.gbif.api.model.collections.AlternativeCode;
import org.gbif.api.model.collections.Institution;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import com.google.common.base.Strings;

/** A converter for {@link Institution#getAlternativeCodes()} */
public class AlternativeCodesTypeHandler extends BaseTypeHandler<List<AlternativeCode>> {

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, List<AlternativeCode> parameter, JdbcType jdbcType)
      throws SQLException {
    Map<String, String> valuesMap = new HashMap<>();
    parameter.forEach(alt -> valuesMap.put(alt.getCode(), alt.getDescription()));
    ps.setString(i, HStoreConverter.toString(valuesMap));
  }

  @Override
  public List<AlternativeCode> getNullableResult(ResultSet rs, String columnName)
      throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public List<AlternativeCode> getNullableResult(ResultSet rs, int columnIndex)
      throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public List<AlternativeCode> getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private List<AlternativeCode> fromString(String hstring) {
    if (!Strings.isNullOrEmpty(hstring)) {
      Map<String, String> valuesMap = HStoreConverter.fromString(hstring);
      List<AlternativeCode> result = new ArrayList<>();
      valuesMap.forEach((k, v) -> result.add(new AlternativeCode(k, v)));

      return result;
    }
    return new ArrayList<>();
  }
}
