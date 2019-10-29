package org.gbif.registry.persistence.mapper.auxhandler;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provides convertion from the key value pairs to HStore.
 * When reading, the caller is guaranteed ordering of the content.
 * This is in a separate package because of auto-mapping problems.
 */
public class SettingsTypeHandler extends BaseTypeHandler<Map<String, String>> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Map<String, String> parameter, JdbcType jdbcType)
    throws SQLException {
    ps.setString(i, HStoreConverter.toString(parameter));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public Map<String, String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public Map<String, String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private TreeMap<String, String> fromString(String hstring) {
    TreeMap<String, String> sortedMap = Maps.newTreeMap();
    if (!Strings.isNullOrEmpty(hstring)) {
      sortedMap.putAll(HStoreConverter.fromString(hstring)); // it is indeed <String,String>
    }
    return sortedMap;
  }
}
