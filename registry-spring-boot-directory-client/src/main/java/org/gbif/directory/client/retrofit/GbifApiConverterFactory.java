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
package org.gbif.directory.client.retrofit;

import org.gbif.api.model.common.paging.PagingRequest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.annotation.Nullable;

import retrofit2.Converter;
import retrofit2.Retrofit;

public class GbifApiConverterFactory extends Converter.Factory {

  @Nullable
  @Override
  public Converter<?, String> stringConverter(
      Type type, Annotation[] annotations, Retrofit retrofit) {

    if (type instanceof PagingRequest) {
      return value -> toQueryString((PagingRequest) value);
    }
    return super.stringConverter(type, annotations, retrofit);
  }

  private static String toQueryString(PagingRequest pagingRequest) {
    return "limit=" + pagingRequest.getLimit() + "&offset=" + pagingRequest.getOffset();
  }
}
