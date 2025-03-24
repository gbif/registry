package org.gbif.registry.persistence.mapper.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;

public class MachineDescriptorTypeHandler extends BaseTypeHandler<JsonNode> {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, JsonNode parameter, JdbcType jdbcType) throws SQLException {
    try {
      ps.setObject(i, objectMapper.writeValueAsString(parameter), Types.OTHER); // Store as JSONB
    } catch (JsonProcessingException e) {
      throw new SQLException("Error serializing JsonNode to JSON", e);
    }
  }

  @Override
  public JsonNode getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String json = rs.getString(columnName);
    return parseJson(json);
  }

  @Override
  public JsonNode getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String json = rs.getString(columnIndex);
    return parseJson(json);
  }

  @Override
  public JsonNode getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String json = cs.getString(columnIndex);
    return parseJson(json);
  }

  private JsonNode parseJson(String json) throws SQLException {
    try {
      return json != null ? objectMapper.readTree(json) : null;
    } catch (JsonProcessingException e) {
      throw new SQLException("Error deserializing JSON to JsonNode", e);
    }
  }
}
