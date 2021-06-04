/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.registry.persistence.mapper.handler;

import org.gbif.api.model.collections.suggestions.Change;
import org.gbif.registry.persistence.mapper.collections.dto.ChangeDto;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionLikeType;
import com.google.common.base.Strings;

/** {@link org.apache.ibatis.type.TypeHandler} for arrays of {@link Change}. */
public class SuggestedChangesTypeHandler extends BaseTypeHandler<Set<ChangeDto>> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, Set<ChangeDto> parameter, JdbcType jdbcType)
      throws SQLException {
    ps.setString(i, toString(parameter));
  }

  @Override
  public Set<ChangeDto> getNullableResult(ResultSet resultSet, String columnName)
      throws SQLException {
    return fromString(resultSet.getString(columnName));
  }

  @Override
  public Set<ChangeDto> getNullableResult(ResultSet resultSet, int columnIndex)
      throws SQLException {
    return fromString(resultSet.getString(columnIndex));
  }

  @Override
  public Set<ChangeDto> getNullableResult(CallableStatement callableStatement, int columnIndex)
      throws SQLException {
    return fromString(callableStatement.getString(columnIndex));
  }

  private String toString(Set<ChangeDto> changesList) {
    try {
      return OBJECT_MAPPER.writeValueAsString(changesList);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Couldn't convert changes list to JSON: " + changesList.toString(), e);
    }
  }

  private Set<ChangeDto> fromString(String json) {
    if (Strings.isNullOrEmpty(json)) {
      return new HashSet<>();
    }

    try {
      JsonNode root = OBJECT_MAPPER.readTree(json);
      Set<ChangeDto> dtos = new HashSet<>();
      for (JsonNode element : root) {
        ChangeDto changeDto = new ChangeDto();
        changeDto.setFieldName(element.get("fieldName").asText());

        Class<?> fieldType = parseFieldType(element.get("fieldType").asText());
        changeDto.setFieldType(fieldType);

        if (Collection.class.isAssignableFrom(fieldType)
            && element.hasNonNull("fieldGenericTypeName")) {
          changeDto.setFieldGenericTypeName(element.get("fieldGenericTypeName").asText());
          Class<?> genericType = Class.forName(element.get("fieldGenericTypeName").asText());
          CollectionLikeType collectionLikeType =
              OBJECT_MAPPER.getTypeFactory().constructCollectionLikeType(fieldType, genericType);
          changeDto.setSuggested(
              OBJECT_MAPPER.readValue(element.get("suggested").toString(), collectionLikeType));
          changeDto.setPrevious(
              OBJECT_MAPPER.readValue(element.get("previous").toString(), collectionLikeType));
        } else {
          changeDto.setSuggested(
              OBJECT_MAPPER.readValue(
                  element.get("suggested").toString(), changeDto.getFieldType()));
          changeDto.setPrevious(
              OBJECT_MAPPER.readValue(
                  element.get("previous").toString(), changeDto.getFieldType()));
        }

        changeDto.setCreated(
            OBJECT_MAPPER.readValue(element.get("created").toString(), Date.class));
        if (!element.get("author").isNull()) {
          changeDto.setAuthor(element.get("author").asText());
        }
        changeDto.setOverwritten(element.get("overwritten").asBoolean());

        dtos.add(changeDto);
      }

      return dtos;
    } catch (IOException | ClassNotFoundException e) {
      throw new IllegalStateException("Couldn't deserialize JSON from DB: " + json, e);
    }
  }

  private Class<?> parseFieldType(String type) throws ClassNotFoundException {
    switch (type) {
      case "boolean":
        return boolean.class;
      case "int":
        return int.class;
      case "long":
        return long.class;
      default:
        return Class.forName(type);
    }
  }
}
