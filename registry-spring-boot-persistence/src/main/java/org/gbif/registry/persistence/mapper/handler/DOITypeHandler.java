package org.gbif.registry.persistence.mapper.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.gbif.api.model.common.DOI;

import javax.annotation.Nullable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * A converter for DOI objects to the database representation using the DOI name.
 * Nulls are passed through.
 */
@MappedTypes({DOI.class})
public class DOITypeHandler implements TypeHandler<DOI> {

  @Override
  public void setParameter(PreparedStatement ps, int i, DOI parameter, JdbcType jdbcType) throws SQLException {
    ps.setObject(i, parameter == null ? null : parameter.getDoiName(), Types.OTHER);
  }

  @Override
  public DOI getResult(ResultSet rs, int columnIndex) throws SQLException {
    return newDOIInstance(rs.getString(columnIndex));
  }

  @Override
  public DOI getResult(ResultSet rs, String columnName) throws SQLException {
    return newDOIInstance(rs.getString(columnName));
  }

  @Override
  public DOI getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return newDOIInstance(cs.getString(columnIndex));
  }

  private DOI newDOIInstance(@Nullable String doi) {
    return doi == null ? null : new DOI(doi);
  }
}
