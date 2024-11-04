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
package org.gbif.registry.ws.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import java.util.*;
import org.gbif.checklistbank.ws.client.NubResourceClient;
import org.gbif.registry.domain.ws.*;
import org.gbif.registry.security.precheck.AuthPreCheckInterceptor;
import org.gbif.registry.ws.converter.CountryMessageConverter;
import org.gbif.registry.ws.converter.UuidTextMessageConverter;
import org.gbif.registry.ws.provider.CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver;
import org.gbif.registry.ws.provider.CollectionSearchRequestHandlerMethodArgumentResolver;
import org.gbif.registry.ws.provider.CountryListHandlerMethodArgumentResolver;
import org.gbif.registry.ws.provider.InstitutionFacetedSearchRequestHandlerMethodArgumentResolver;
import org.gbif.registry.ws.provider.InstitutionSearchRequestHandlerMethodArgumentResolver;
import org.gbif.registry.ws.provider.PartialDateHandlerMethodArgumentResolver;
import org.gbif.registry.ws.provider.networkEntitiesList.*;
import org.gbif.vocabulary.client.ConceptClient;
import org.gbif.ws.client.ClientBuilder;
import org.gbif.ws.json.JacksonJsonObjectMapperProvider;
import org.gbif.ws.server.processor.ParamNameProcessor;
import org.gbif.ws.server.provider.CountryHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSearchRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSuggestRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new AuthPreCheckInterceptor());
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new PageableHandlerMethodArgumentResolver());
    argumentResolvers.add(new CountryHandlerMethodArgumentResolver());
    argumentResolvers.add(new CountryListHandlerMethodArgumentResolver());
    argumentResolvers.add(new PartialDateHandlerMethodArgumentResolver());
    argumentResolvers.add(new DatasetSearchRequestHandlerMethodArgumentResolver());
    argumentResolvers.add(new DatasetSuggestRequestHandlerMethodArgumentResolver());
    argumentResolvers.add(new DatasetRequestSearchParamsHandlerMethodArgumentResolver());
    argumentResolvers.add(new OrganizationRequestSearchParamsHandlerMethodArgumentResolver());
    argumentResolvers.add(new InstallationRequestSearchParamsHandlerMethodArgumentResolver());
    argumentResolvers.add(new NetworkRequestSearchParamsHandlerMethodArgumentResolver());
    argumentResolvers.add(new NodeRequestSearchParamsHandlerMethodArgumentResolver());
    argumentResolvers.add(new InstitutionSearchRequestHandlerMethodArgumentResolver());
    argumentResolvers.add(new InstitutionFacetedSearchRequestHandlerMethodArgumentResolver());
    argumentResolvers.add(new CollectionSearchRequestHandlerMethodArgumentResolver());
    argumentResolvers.add(new CollectionDescriptorsSearchRequestHandlerMethodArgumentResolver());
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

  @Bean
  public ConceptClient conceptClient(@Value("${api.root.url}") String apiRootUrl) {
    return new ClientBuilder()
        .withObjectMapper(
            JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()
                .registerModule(new JavaTimeModule()))
        .withUrl(apiRootUrl)
        .build(ConceptClient.class);
  }

  @Bean
  public NubResourceClient nubResourceClient(@Value("${api.root.url}") String apiRootUrl) {
    return new ClientBuilder()
        .withObjectMapper(
            JacksonJsonObjectMapperProvider.getObjectMapperWithBuilderSupport()
                .registerModule(new JavaTimeModule()))
        .withUrl(apiRootUrl)
        .build(NubResourceClient.class);
  }

  @Bean
  public CountryMessageConverter countryMessageConverter() {
    return new CountryMessageConverter();
  }
}
