package org.gbif.registry.ws.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.processor.ParamNameProcessor;
import org.gbif.registry.ws.annotation.ParamName;
import org.gbif.registry.ws.converter.UuidTextMessageConverter;
import org.gbif.registry.ws.provider.PartialDateHandlerMethodArgumentResolver;
import org.gbif.ws.mixin.LicenseMixin;
import org.gbif.ws.server.provider.CountryHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSearchRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSuggestRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
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

  /**
   * Processor for annotation {@link ParamName}.
   *
   * @return ParamNameProcessor
   */
  @Bean
  protected ParamNameProcessor paramNameProcessor() {
    return new ParamNameProcessor();
  }

  /**
   * Custom {@link BeanPostProcessor} for adding {@link ParamNameProcessor}.
   *
   * @return BeanPostProcessor
   */
  @Bean
  public BeanPostProcessor beanPostProcessor() {
    return new BeanPostProcessor() {

      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RequestMappingHandlerAdapter) {
          RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
          List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>(adapter.getArgumentResolvers());
          argumentResolvers.add(0, paramNameProcessor());
          adapter.setArgumentResolvers(argumentResolvers);
        }
        return bean;
      }
    };
  }

  @Bean
  public ObjectMapper titleLookupObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.addMixIn(Dataset.class, LicenseMixin.class);

    return objectMapper;
  }

  @Bean
  public RestTemplate titleLookupRestTemplate() {
    final RestTemplate restTemplate = new RestTemplate();
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(titleLookupObjectMapper());
    restTemplate.getMessageConverters().add(0, converter);

    return restTemplate;
  }
}