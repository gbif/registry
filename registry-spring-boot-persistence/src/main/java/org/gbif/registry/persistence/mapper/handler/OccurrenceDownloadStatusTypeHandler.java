package org.gbif.registry.persistence.mapper.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.gbif.api.model.occurrence.Download.Status;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class OccurrenceDownloadStatusTypeHandler implements TypeHandler<Status> {

  @Override
  public void setParameter(PreparedStatement ps, int i, Status parameter, JdbcType jdbcType) throws SQLException {
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
