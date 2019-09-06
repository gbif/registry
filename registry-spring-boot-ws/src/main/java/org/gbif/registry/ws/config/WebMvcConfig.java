package org.gbif.registry.ws.config;

import org.gbif.registry.ws.converter.UuidTextMessageConverter;
import org.gbif.registry.ws.provider.PartialDateHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.CountryHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSearchRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSuggestRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new PageableHandlerMethodArgumentResolver());
    argumentResolvers.add(new CountryHandlerMethodArgumentResolver());
    argumentResolvers.add(new PartialDateHandlerMethodArgumentResolver());
    argumentResolvers.add(new DatasetSearchRequestHandlerMethodArgumentResolver());
    argumentResolvers.add(new DatasetSuggestRequestHandlerMethodArgumentResolver());
  }

  @Override
  public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    converters.add(new UuidTextMessageConverter());
  }
}