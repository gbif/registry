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
package org.gbif.registry.ws;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.api.vocabulary.UserRole;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.InMemoryEmailSender;
import org.gbif.registry.message.MessagePublisherStub;
import org.gbif.registry.search.DatasetSearchServiceStub;
import org.gbif.registry.search.dataset.indexing.es.EsConfiguration;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsClient;
import org.gbif.registry.search.dataset.indexing.ws.GbifWsRetrofitClient;
import org.gbif.registry.ws.config.DataSourcesConfiguration;
import org.gbif.ws.client.filter.SimplePrincipalProvider;

import java.util.Collections;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.context.support.GenericWebApplicationContext;

@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(
    basePackages = {
      "org.gbif.ws.server.interceptor",
      "org.gbif.ws.server.aspect",
      "org.gbif.ws.server.filter",
      "org.gbif.ws.server.advice",
      "org.gbif.ws.server.mapper",
      "org.gbif.ws.security",
      "org.gbif.registry.search.dataset.service",
      "org.gbif.registry.search.dataset.indexing",
      "org.gbif.registry.search",
      "org.gbif.registry.ws.advice",
      "org.gbif.registry.ws.config",
      "org.gbif.registry.ws.resources",
      "org.gbif.registry.security",
      "org.gbif.registry.ws.surety",
      "org.gbif.registry.persistence",
      "org.gbif.registry.identity",
      "org.gbif.registry.surety",
      "org.gbif.registry.mail",
      "org.gbif.registry.doi",
      "org.gbif.registry.pipelines",
      "org.gbif.registry.directory",
      "org.gbif.registry.events",
      "org.gbif.registry.messaging",
      "org.gbif.registry.test"
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = {
            EsConfiguration.class,
            DataSourcesConfiguration.class,
            FlywayAutoConfiguration.FlywayConfiguration.class,
            FlywayAutoConfiguration.class,
            RegistryWsApplication.class
          })
    })
@PropertySource(RegistryIntegrationTestsConfiguration.TEST_PROPERTIES)
public class RegistryIntegrationTestsConfiguration {

  private static final Logger LOG =
      LoggerFactory.getLogger(RegistryIntegrationTestsConfiguration.class);

  public static final String TEST_PROPERTIES = "classpath:application-test.yml";

  // use InMemoryEmailSender if devemail is disabled
  @Bean
  @ConditionalOnProperty(value = "mail.devemail.enabled", havingValue = "false")
  public EmailSender emailSender() {
    LOG.info("ImMemoryEmailSender (stub) activated");
    return new InMemoryEmailSender();
  }

  // use stub instead of rabbit MQ if message is disabled
  @Bean
  @ConditionalOnProperty(value = "message.enabled", havingValue = "false")
  public MessagePublisher testMessagePublisher() {
    LOG.info("MessagePublisherStub activated");
    return new MessagePublisherStub();
  }

  // use stub instead dataset search
  // @Bean TODO: Ignore this injector, the realtime indexer has to be refactored
  public DatasetSearchService datasetSearchService() {
    return new DatasetSearchServiceStub();
  }

  public GbifWsClient gbifWsClient(
    GenericWebApplicationContext genericWebApplicationContext, @Qualifier("apiMapper") ObjectMapper objectMapper) {
    return new GbifWsRetrofitClient("http://localhost:" + ((AnnotationConfigServletWebServerApplicationContext) genericWebApplicationContext).getWebServer().getPort(), objectMapper);
  }

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
  public SimplePrincipalProvider simplePrincipalProvider() {
    SimplePrincipalProvider simplePrincipalProvider = new SimplePrincipalProvider();
    simplePrincipalProvider.setPrincipal("WS TEST");
    setSecurityPrincipal(simplePrincipalProvider, UserRole.REGISTRY_ADMIN);
    return simplePrincipalProvider;
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
}
