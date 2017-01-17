package org.gbif.identity.mybatis;

import org.gbif.api.vocabulary.UserRole;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.postgresql.util.HStoreConverter;

/**
 * Provides convertion from the key value pairs to HStore.
 * When reading, the caller is guaranteed ordering of the content.
 */
public class SettingsTypeHandler extends BaseTypeHandler<Map<String, String>> {
  private static final Logger LOG = LoggerFactory.getLogger(SettingsTypeHandler.class);

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
    throws SQLException {
    ps.setString(i, HStoreConverter.toString(parameter));
  }

  @Override
  public TreeMap<String,String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public TreeMap<String,String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public TreeMap<String,String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private TreeMap<String,String> fromString(String hstring) {
    TreeMap<String,String> sortedMap = Maps.newTreeMap();
    if (!Strings.isNullOrEmpty(hstring)) {
      sortedMap.putAll(HStoreConverter.fromString(hstring)); // it is indeed <String,String>
    }
    return sortedMap;
  }
}
