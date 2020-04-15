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
package org.gbif.registry.ws.config;

import org.gbif.registry.domain.ws.ErrorResponse;
import org.gbif.registry.domain.ws.IptEntityResponse;
import org.gbif.registry.domain.ws.LegacyDataset;
import org.gbif.registry.domain.ws.LegacyDatasetResponse;
import org.gbif.registry.domain.ws.LegacyDatasetResponseListWrapper;
import org.gbif.registry.domain.ws.LegacyEndpoint;
import org.gbif.registry.domain.ws.LegacyEndpointResponse;
import org.gbif.registry.domain.ws.LegacyEndpointResponseListWrapper;
import org.gbif.registry.domain.ws.LegacyInstallation;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponse;
import org.gbif.registry.domain.ws.LegacyOrganizationBriefResponseListWrapper;
import org.gbif.registry.domain.ws.LegacyOrganizationResponse;
import org.gbif.registry.ws.converter.UuidTextMessageConverter;
import org.gbif.registry.ws.provider.PartialDateHandlerMethodArgumentResolver;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.server.processor.ParamNameProcessor;
import org.gbif.ws.server.provider.CountryHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSearchRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSuggestRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

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
    converters.add(marshallingMessageConverter());
  }

  /**
   * Processor for annotation {@link org.gbif.api.annotation.ParamName}.
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
      public Object postProcessBeforeInitialization(@NotNull Object bean, String beanName) {
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(@NotNull Object bean, String beanName) {
        if (bean instanceof RequestMappingHandlerAdapter) {
          RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
          List<HandlerMethodArgumentResolver> nullSafeArgumentResolvers =
              Optional.ofNullable(adapter.getArgumentResolvers()).orElse(Collections.emptyList());
          List<HandlerMethodArgumentResolver> argumentResolvers =
              new ArrayList<>(nullSafeArgumentResolvers);
          argumentResolvers.add(0, paramNameProcessor());
          adapter.setArgumentResolvers(argumentResolvers);
        }
        return bean;
      }
    };
  }

  @Primary
  @Bean
  public ObjectMapper registryObjectMapper() {
    return JacksonJsonObjectMapperProvider.getObjectMapper();
  }

  @Bean
  public XmlMapper xmlMapper() {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    xmlMapper.registerModules(Arrays.asList(new SimpleModule(), new JaxbAnnotationModule()));
    return xmlMapper;
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer customJson() {
    return builder -> builder.modulesToInstall(new JaxbAnnotationModule());
  }

  @Bean
  public MarshallingHttpMessageConverter marshallingMessageConverter() {
    MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();
    converter.setMarshaller(jaxbMarshaller());
    converter.setUnmarshaller(jaxbMarshaller());
    return converter;
  }

  @Bean
  public Jaxb2Marshaller jaxbMarshaller() {
    Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
    marshaller.setClassesToBeBound(
        LegacyEndpoint.class,
        LegacyEndpointResponse.class,
        LegacyEndpointResponseListWrapper.class,
        LegacyInstallation.class,
        LegacyOrganizationResponse.class,
        LegacyOrganizationBriefResponseListWrapper.class,
        LegacyOrganizationBriefResponse.class,
        LegacyDataset.class,
        LegacyDatasetResponse.class,
        LegacyDatasetResponseListWrapper.class,
        IptEntityResponse.class,
        ErrorResponse.class);
    return marshaller;
  }
}
