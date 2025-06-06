package org.gbif.registry.persistence.facet;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler for PostgreSQL LTREE type.
 * Handles conversion between LTREE database type and Java String.
 * This handler is only used when explicitly specified in mapper XML files.
 * Located in a separate package to avoid auto-scanning by MyBatis.
 */
public class LtreeTypeHandler extends BaseTypeHandler<String> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
      throws SQLException {
    PGobject ltreeObject = new PGobject();
    ltreeObject.setType("ltree");
    ltreeObject.setValue(parameter);
    ps.setObject(i, ltreeObject);
  }

  @Override
  public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return rs.getString(columnName);
  }

  @Override
  public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return rs.getString(columnIndex);
  }

  @Override
  public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return cs.getString(columnIndex);
  }
} 