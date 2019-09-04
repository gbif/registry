package org.gbif.ws.server.provider;

import org.gbif.api.model.registry.search.DatasetSearchRequest;
import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class DatasetSearchRequestHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return DatasetSearchRequest.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    final DatasetSearchRequest result = new DatasetSearchRequest();

    final String countryCode = webRequest.getParameter("country");
    if (countryCode != null) {
      Country country = Country.fromIsoCode(countryCode);

      if (country == null) {
        // if nothing found also try by enum name
        country = VocabularyUtils.lookupEnum(countryCode, Country.class);
      }

      result.addCountryFilter(country);
    }

    return result;
  }
}
