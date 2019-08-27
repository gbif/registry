package org.gbif.ws.server.provider;

import org.gbif.api.model.common.paging.Pageable;
import org.gbif.api.model.common.paging.PagingRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.DEFAULT_PARAM_OFFSET;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_LIMIT;
import static org.gbif.api.model.common.paging.PagingConstants.PARAM_OFFSET;

public class PageableHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return Pageable.class.equals(parameter.getParameterType());
  }

  @Override
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
    final int limit = Optional.ofNullable(webRequest.getParameter(PARAM_LIMIT))
        .map(Integer::parseInt)
        .orElse(DEFAULT_PARAM_LIMIT);
    final long offset = Optional.ofNullable(webRequest.getParameter(PARAM_OFFSET))
        .map(Long::parseLong)
        .orElse(DEFAULT_PARAM_OFFSET);

    return new PagingRequest(offset, limit);
  }
}
