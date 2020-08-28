package org.gbif.registry.persistence.mapper.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

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
    return Locale.forLanguageTag(rs.getString(columnIndex));
  }

  @Override
  public Locale getResult(ResultSet rs, String columnName) throws SQLException {
    return Locale.forLanguageTag(rs.getString(columnName));
  }

  @Override
  public Locale getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return Locale.forLanguageTag(cs.getString(columnIndex));
  }
}
