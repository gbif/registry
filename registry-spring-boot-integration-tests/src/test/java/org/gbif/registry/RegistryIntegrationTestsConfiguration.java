package org.gbif.registry;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;

import org.gbif.api.service.registry.DatasetSearchService;
import org.gbif.common.messaging.api.MessagePublisher;
import org.gbif.registry.mail.EmailSender;
import org.gbif.registry.mail.InMemoryEmailSender;
import org.gbif.registry.message.MessagePublisherStub;
import org.gbif.registry.search.DatasetSearchServiceStub;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import java.util.Date;

@TestConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {
  "org.gbif.ws.server.interceptor",
  "org.gbif.ws.server.filter",
  "org.gbif.ws.security",
  "org.gbif.registry",
  "org.gbif.registry.ws.security",
})
@PropertySource("classpath:application-test.yml")
public class RegistryIntegrationTestsConfiguration {

  // use InMemoryEmailSender if devemail is disabled
  @Bean
  @Primary
  @ConditionalOnProperty(value = "mail.devemail.enabled", havingValue = "false")
  public EmailSender emailSender() {
    return new InMemoryEmailSender();
  }

  // use stub instead of rabbit MQ if message is disabled
  @Bean
  @Primary
  @ConditionalOnProperty(value = "message.enabled", havingValue = "false")
  public MessagePublisher messagePublisher() {
    return new MessagePublisherStub();
  }


  // use stub instead dataset search
  @Bean
  @Primary
  public DatasetSearchService datasetSearchService() {
    return new DatasetSearchServiceStub();
  }

  @Bean
  public BeanUtilsBean beanUtilsBean() {
    DateTimeConverter dateConverter = new DateConverter(null);
    dateConverter.setPatterns(new String[]{"dd-MM-yyyy"});
    ConvertUtils.register(dateConverter, Date.class);

    ConvertUtilsBean convertUtilsBean = new ConvertUtilsBean() {
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
}
