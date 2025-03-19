package org.gbif.registry.ws.client;

import feign.Logger;
import feign.RequestInterceptor;
import feign.Retryer;
import org.gbif.api.model.registry.metasync.cetaf.CetafCollectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "cetaf",
    url = "${cetaf.api.url}",
    configuration = CetafClient.CetafClientConfiguration.class
)
public interface CetafClient {

    @GetMapping("/")
    CetafCollectionResponse getCollectionById(
        @RequestParam("operation") String operation,
        @RequestParam("protocol") String protocol,
        @RequestParam("values") String sourceId
    );

    @Configuration
    class CetafClientConfiguration {
        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.FULL;
        }

        @Bean
        public Retryer retryer() {
            return new Retryer.Default(100, 1000, 3);
        }

    }
}
