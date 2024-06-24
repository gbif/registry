package org.gbif.registry.persistence.mapper.auxhandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gbif.api.vocabulary.collections.InstitutionType;

public class InstitutionTypeHandler extends BaseTypeHandler<List<InstitutionType>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, List<InstitutionType> parameter, JdbcType jdbcType) throws SQLException {
    Array array = ps.getConnection().createArrayOf("enum_institution_type", parameter.toArray());
    ps.setArray(i, array);
  }

  @Override
  public List<InstitutionType> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return mapToArray(rs.getArray(columnName));
  }

  @Override
  public List<InstitutionType> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return mapToArray(rs.getArray(columnIndex));
  }

  @Override
  public List<InstitutionType> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return mapToArray(cs.getArray(columnIndex));
  }

  private List<InstitutionType> mapToArray(Array sqlArray) throws SQLException {
    if (sqlArray == null) {
      return null;
    }
    return Arrays.stream((Object[]) sqlArray.getArray())
      .map(Object::toString)
      .map(String::toUpperCase)
      .map(InstitutionType::valueOf)
      .collect(Collectors.toList());
  }
}
