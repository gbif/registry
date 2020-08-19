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
package org.gbif.registry.ws.it;

import org.gbif.api.vocabulary.UserRole;
import org.gbif.registry.doi.config.TitleLookupConfiguration;
import org.gbif.registry.events.config.VarnishPurgeConfiguration;
import org.gbif.registry.mail.EmailSenderImpl;
import org.gbif.registry.mail.config.OrganizationSuretyMailConfigurationProperties;
import org.gbif.registry.search.dataset.indexing.checklistbank.ChecklistbankPersistenceServiceImpl;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.surety.OrganizationEmailTemplateManagerIT;
import org.gbif.registry.ws.config.DataSourcesConfiguration;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.ribbon.FeignRibbonClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.zaxxer.hikari.HikariDataSource;

@TestConfiguration
@SpringBootApplication(exclude = RabbitAutoConfiguration.class)
@MapperScan("org.gbif.registry.persistence.mapper")
@ComponentScan(
    basePackages = {
      "org.gbif.ws.server.interceptor",
      "org.gbif.ws.server.aspect",
      "org.gbif.ws.server.filter",
      "org.gbif.ws.server.advice",
      "org.gbif.ws.server.mapper",
      "org.gbif.ws.security",
      "org.gbif.registry.search.dataset.indexing",
      "org.gbif.registry.search.dataset.service",
      "org.gbif.registry.search",
      "org.gbif.registry.search.test",
      "org.gbif.registry.ws.client",
      "org.gbif.registry.ws.advice",
      "org.gbif.registry.ws.config",
      "org.gbif.registry.ws.resources",
      "org.gbif.registry.ws.surety",
      "org.gbif.registry.ws.it.fixtures",
      "org.gbif.registry.security",
      "org.gbif.registry.persistence",
      "org.gbif.registry.identity",
      "org.gbif.registry.surety",
      "org.gbif.registry.mail",
      "org.gbif.registry.doi",
      "org.gbif.registry.pipelines",
      "org.gbif.registry.directory",
      "org.gbif.registry.events",
      "org.gbif.registry.oaipmh",
      "org.gbif.registry.service",
      "org.gbif.registry.test"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = {
            EmailSenderImpl.class,
            GbifWsClient.class,
            VarnishPurgeConfiguration.class,
            TitleLookupConfiguration.class,
            OrganizationSuretyMailConfigurationProperties.class,
            ChecklistbankPersistenceServiceImpl.class,
            DataSourcesConfiguration.class,
            OrganizationEmailTemplateManagerIT.OrganizationEmailTemplateManagerTestConfiguration
                .class
          })
    })
@PropertySource(RegistryIntegrationTestsConfiguration.TEST_PROPERTIES)
@ImportAutoConfiguration({
  HttpMessageConvertersAutoConfiguration.class,
  RibbonAutoConfiguration.class,
  FeignRibbonClientAutoConfiguration.class,
  FeignAutoConfiguration.class
})
@ActiveProfiles("test")
public class RegistryIntegrationTestsConfiguration {

  public static final String TEST_PROPERTIES = "classpath:application-test.yml";

  @Bean
  public BeanUtilsBean beanUtilsBean() {
    DateTimeConverter dateConverter = new DateConverter(null);
    dateConverter.setPatterns(new String[] {"dd-MM-yyyy"});
    ConvertUtils.register(dateConverter, Date.class);

    ConvertUtilsBean convertUtilsBean =
        new ConvertUtilsBean() {
          @Override
          public Object convert(String value, Class clazz) {
            if (clazz.isEnum()) {
              return Enum.valueOf(clazz, value);
            } else {
              return super.convert(value, clazz);
            }
          }
        };

    convertUtilsBean.register(dateConverter, Date.class);

    return new BeanUtilsBean(convertUtilsBean);
  }

  @Bean
  @Primary
  @ConfigurationProperties(DataSourcesConfiguration.REGISTRY_DATASOURCE_PREFIX + ".datasource")
  public DataSourceProperties registryDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  @ConfigurationProperties("registry.datasource.hikari")
  public HikariDataSource registryDataSource() {
    return registryDataSourceProperties()
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Bean
  public SimplePrincipalProvider simplePrincipalProvider() {
    SimplePrincipalProvider simplePrincipalProvider = new SimplePrincipalProvider();
    simplePrincipalProvider.setPrincipal("WS TEST");
    setSecurityPrincipal(simplePrincipalProvider, UserRole.REGISTRY_ADMIN);
    return simplePrincipalProvider;
  }

  @Bean
  public WebMvcRegistrations feignWebRegistrations() {
    return new WebMvcRegistrations() {
      @Override
      public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new FeignFilterRequestMappingHandlerMapping();
      }
    };
  }

  private static class FeignFilterRequestMappingHandlerMapping
      extends RequestMappingHandlerMapping {
    @Override
    protected boolean isHandler(Class<?> beanType) {
      return super.isHandler(beanType)
          && (AnnotationUtils.findAnnotation(beanType, FeignClient.class) == null);
    }
  }

  public static void setSecurityPrincipal(
      SimplePrincipalProvider simplePrincipalProvider, UserRole userRole) {
    SecurityContext ctx = SecurityContextHolder.createEmptyContext();
    SecurityContextHolder.setContext(ctx);

    ctx.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            simplePrincipalProvider.get().getName(),
            "",
            Collections.singleton(new SimpleGrantedAuthority(userRole.name()))));
  }

  public static void main(String[] args) {
    SpringApplication.run(RegistryIntegrationTestsConfiguration.class, args);
  }
}
