/*
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
package org.gbif.registry.ws.provider;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;

import java.util.Arrays;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CountryListHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return "Country[]".equals(parameter.getParameterType().getSimpleName());
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {
    final String paramName = parameter.getParameterName();
    final String[] countryCodes =
        paramName != null ? webRequest.getParameterMap().get(paramName) : null;
    return countryCodes != null
        ? Arrays.stream(countryCodes).map(this::parseCountry).toArray(Country[]::new)
        : null;
  }

  private Country parseCountry(String param) {
    Country parsed = Country.fromIsoCode(param);

    if (parsed == null) {
      // if nothing found also try by enum name
      parsed = VocabularyUtils.lookupEnum(param, Country.class);
    }
    return parsed;
  }
}
