package org.gbif.registry.persistence.mapper.handler;

import com.google.common.collect.Sets;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.gbif.api.vocabulary.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class UserRoleTypeHandler extends BaseTypeHandler<Set<UserRole>> {
  private static final Logger LOG = LoggerFactory.getLogger(UserRoleTypeHandler.class);

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Set<UserRole> parameter, JdbcType jdbcType) throws
      SQLException {
    ps.setArray(i, ps.getConnection().createArrayOf("text", parameter.toArray()));
  }

  @Override
  public Set<UserRole> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return toSet(rs.getArray(columnName));
  }

  @Override
  public Set<UserRole> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return toSet(rs.getArray(columnIndex));
  }

  @Override
  public Set<UserRole> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return toSet(cs.getArray(columnIndex));
  }

  private Set<UserRole> toSet(Array pgArray) throws SQLException {
    if (pgArray == null) {
      return Sets.newHashSet();
    }

    String[] strings = (String[]) pgArray.getArray();
    if (strings.length == 0) return Sets.newHashSet();

    Set<UserRole> result = Sets.newHashSet();
    for (String s : strings) {
      try {
        result.add(UserRole.valueOf(s));
      } catch (IllegalArgumentException e) {
        LOG.warn("Database contains unsupported UserRole values which are ignored: {}", s);
      }
    }
    return result;
  }
}
