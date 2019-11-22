package org.gbif.registry.ws.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.gbif.api.model.registry.Dataset;
import org.gbif.registry.processor.ParamNameProcessor;
import org.gbif.registry.ws.annotation.ParamName;
import org.gbif.registry.ws.converter.UuidTextMessageConverter;
import org.gbif.registry.ws.model.LegacyEndpointListWrapper;
import org.gbif.registry.ws.model.LegacyEndpointResponse;
import org.gbif.registry.ws.model.LegacyOrganizationBriefResponse;
import org.gbif.registry.ws.provider.PartialDateHandlerMethodArgumentResolver;
import org.gbif.ws.mixin.LicenseMixin;
import org.gbif.ws.server.provider.CountryHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSearchRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.DatasetSuggestRequestHandlerMethodArgumentResolver;
import org.gbif.ws.server.provider.PageableHandlerMethodArgumentResolver;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MarshallingHttpMessageConverter;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
      public Object postProcessBeforeInitialization(@NotNull Object bean, String beanName) {
        return bean;
      }

      @Override
      public Object postProcessAfterInitialization(@NotNull Object bean, String beanName) {
        if (bean instanceof RequestMappingHandlerAdapter) {
          RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
          List<HandlerMethodArgumentResolver> nullSafeArgumentResolvers = Optional.ofNullable(adapter.getArgumentResolvers())
            .orElse(Collections.emptyList());
          List<HandlerMethodArgumentResolver> argumentResolvers = new ArrayList<>(nullSafeArgumentResolvers);
          argumentResolvers.add(0, paramNameProcessor());
          adapter.setArgumentResolvers(argumentResolvers);
        }
        return bean;
      }
    };
  }

  @Primary
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
  public XmlMapper xmlMapper() {
    XmlMapper xmlMapper = new XmlMapper();

    ArrayList<Module> modules = new ArrayList<>();

    SimpleModule module = new SimpleModule();
    module.addSerializer(LegacyOrganizationBriefResponse[].class, new LegacyOrganizationBriefResponse.LegacyOrganizationArraySerializer());
    modules.add(module);
    modules.add(new JaxbAnnotationModule());

    xmlMapper.registerModules(modules);

    return xmlMapper;
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer customJson() {
    return builder -> {
      builder.serializerByType(LegacyOrganizationBriefResponse[].class, new LegacyOrganizationBriefResponse.LegacyOrganizationArraySerializer());
      builder.modulesToInstall(new JaxbAnnotationModule());
    };
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
    marshaller.setClassesToBeBound(LegacyEndpointListWrapper.class, LegacyEndpointResponse.class);
    return marshaller;
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
