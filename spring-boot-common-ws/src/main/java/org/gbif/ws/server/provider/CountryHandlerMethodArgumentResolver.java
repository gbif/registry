package org.gbif.ws.server.provider;

import org.gbif.api.util.VocabularyUtils;
import org.gbif.api.vocabulary.Country;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CountryHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Country.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    final String paramName = parameter.getParameter().getName();
    final String countryCode = webRequest.getParameter(paramName);
    Country result = Country.fromIsoCode(countryCode);

    if (result == null) {
      // if nothing found also try by enum name
      result = VocabularyUtils.lookupEnum(countryCode, Country.class);
    }

    return result;
  }
}
