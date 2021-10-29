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

import org.gbif.api.model.occurrence.Download.Status;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

public class OccurrenceDownloadStatusTypeHandler implements TypeHandler<Status> {

  @Override
  public void setParameter(PreparedStatement ps, int i, Status parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setObject(i, parameter == null ? null : parameter.name(), Types.OTHER);
  }

  @Override
  public Status getResult(ResultSet rs, String columnName) throws SQLException {
    return Status.valueOf(rs.getString(columnName));
  }

  @Override
  public Status getResult(ResultSet rs, int columnIndex) throws SQLException {
    return Status.valueOf(rs.getString(columnIndex));
  }

  @Override
  public Status getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return Status.valueOf(cs.getString(columnIndex));
  }
}
