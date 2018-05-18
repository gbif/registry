package org.gbif.registry.persistence.mapper.handler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.HStoreConverter;

import static org.gbif.common.messaging.api.messages.DataPrivacyNotificationMessage.EntityType;

/**
 * Provides convertion from the key value pairs to HStore.
 * When reading, the caller is guaranteed ordering of the content.
 */
public class ContextTypeHandler extends BaseTypeHandler<Map<EntityType, List<UUID>>> {

  @Override
  public void setNonNullParameter(
    PreparedStatement ps, int i, Map<EntityType, List<UUID>> parameter, JdbcType jdbcType
  ) throws SQLException {
    ps.setString(i, HStoreConverter.toString(parameter));
  }

  @Override
  public Map<EntityType, List<UUID>> getNullableResult(ResultSet rs, String columnName) throws SQLException {
    return fromString(rs.getString(columnName));
  }

  @Override
  public Map<EntityType, List<UUID>> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    return fromString(rs.getString(columnIndex));
  }

  @Override
  public Map<EntityType, List<UUID>> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    return fromString(cs.getString(columnIndex));
  }

  private Map<EntityType, List<UUID>> fromString(String hstring) {
    Map<EntityType, List<UUID>> map = Maps.newHashMap();
    if (!Strings.isNullOrEmpty(hstring)) {
      HStoreConverter.fromString(hstring).forEach((key, value) -> {
        List<UUID> uuids = new ArrayList<>();
        if (!Strings.isNullOrEmpty(value) && value.length() > 2) {
          Arrays.stream(value.substring(1, value.length() - 1).split(","))
            .forEach(uuidDb -> uuids.add(UUID.fromString(uuidDb)));
        }
        map.put(EntityType.valueOf(key), uuids);
      });
    }
    return map;
  }
}
