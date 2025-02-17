package org.gbif.registry.persistence.mapper.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.gbif.api.model.common.export.ExportFormat;

@MappedTypes(ExportFormat.class)
public class ExportFormatHandler extends BaseTypeHandler<ExportFormat> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, ExportFormat parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, parameter.name());
  }

  @Override
  public ExportFormat getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String value = rs.getString(columnName);
    return value == null ? null : ExportFormat.valueOf(value);
  }

  @Override
  public ExportFormat getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String value = rs.getString(columnIndex);
    return value == null ? null : ExportFormat.valueOf(value);
  }

  @Override
  public ExportFormat getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String value = cs.getString(columnIndex);
    return value == null ? null : ExportFormat.valueOf(value);
  }
}
