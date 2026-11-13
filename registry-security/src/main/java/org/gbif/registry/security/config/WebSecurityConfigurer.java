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
package org.gbif.registry.security.config;

import org.gbif.registry.identity.util.RegistryPasswordEncoder;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.LegacyAuthorizationFilter;
import org.gbif.registry.security.ResourceNotFoundRequestFilter;
import org.gbif.registry.security.WebApplicationExceptionFilter;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter;
import org.gbif.registry.security.jwt.JwtRequestFilter;
import org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter;
import org.gbif.registry.security.util.RequestPathUtils;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true, prePostEnabled = true)
public class WebSecurityConfigurer {

  static final String METADATA_DOCUMENT_CONTENT_SECURITY_POLICY = "default-src 'none'; sandbox";
  private static final Pattern METADATA_DOCUMENT_PATTERN =
      Pattern.compile("^/dataset/(metadata/)?[^/]+/document$", Pattern.CASE_INSENSITIVE);


  private final ApplicationContext context;

  private final UserDetailsService userDetailsService;

  public WebSecurityConfigurer(
      @Qualifier("registryUserDetailsService") UserDetailsService userDetailsService,
      ApplicationContext context) {
    this.userDetailsService = userDetailsService;
    this.context = context;
  }


  @Bean
  static MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
    // Use the default handler provided by Spring Security 6+
    DefaultMethodSecurityExpressionHandler expressionHandler =
      new DefaultMethodSecurityExpressionHandler();

    expressionHandler.setDefaultRolePrefix("");

    return expressionHandler;
  }

  @Bean
  static SecurityContextHolderStrategy securityContextHolderStrategy() {
    SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL);
    return SecurityContextHolder.getContextHolderStrategy();
  }

  @Bean
  public MethodValidationPostProcessor methodValidationPostProcessor() {
    return new MethodValidationPostProcessor();
  }


  @Bean
  public DaoAuthenticationProvider dbAuthenticationProvider() {
    final DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    return firewall;
  }

  /**
   * Main security filter chain for API endpoints.
   */
  @Bean
  @org.springframework.core.annotation.Order(2)
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .securityMatcher(request -> !request.getRequestURI().startsWith("/actuator"))
      .httpBasic(AbstractHttpConfigurer::disable)
      .csrf(AbstractHttpConfigurer::disable)
      // metadata documents are stored as uploaded by publishers and served as XML: a browser
      // opening one must not run scripts it contains
      .headers(headers -> headers.addHeaderWriter(
        new DelegatingRequestMatcherHeaderWriter(
          WebSecurityConfigurer::isMetadataDocumentRequest,
          new StaticHeadersWriter(
            "Content-Security-Policy", METADATA_DOCUMENT_CONTENT_SECURITY_POLICY))))
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .securityContext(securityContext -> securityContext
        .requireExplicitSave(false)
      )
      // endpoints are secured by method security (@Secured, ...). Requests without credentials are
      // anonymous since gbif-common-ws 2.1.4, .authenticated() would reject them on public endpoints
      .authorizeHttpRequests(auth -> auth
        .anyRequest().permitAll()
      );

    // Add filters
    http.addFilterAfter(context.getBean("httpServletRequestWrapperFilter", HttpServletRequestWrapperFilter.class), CsrfFilter.class)
        .addFilterAfter(context.getBean("requestHeaderParamUpdateFilter", RequestHeaderParamUpdateFilter.class), HttpServletRequestWrapperFilter.class)
        .addFilterAfter(context.getBean("identityFilter", IdentityFilter.class), RequestHeaderParamUpdateFilter.class)
        .addFilterAfter(new WebApplicationExceptionFilter(), IdentityFilter.class)
        .addFilterAfter(context.getBean("legacyAuthorizationFilter", LegacyAuthorizationFilter.class), WebApplicationExceptionFilter.class)
        .addFilterAfter(context.getBean("appIdentityFilter", AppIdentityFilter.class), LegacyAuthorizationFilter.class)
        .addFilterAfter(context.getBean("jwtRequestFilter", JwtRequestFilter.class), AppIdentityFilter.class)
        .addFilterAfter(context.getBean("authPreCheckCreationRequestFilter", AuthPreCheckCreationRequestFilter.class), JwtRequestFilter.class)
        .addFilterAfter(context.getBean("editorAuthorizationFilter", EditorAuthorizationFilter.class), AuthPreCheckCreationRequestFilter.class)
        .addFilterAfter(context.getBean("grSciCollEditorAuthorizationFilter", GrSciCollEditorAuthorizationFilter.class), EditorAuthorizationFilter.class)
        .addFilterAfter(context.getBean("resourceNotFoundRequestFilter", ResourceNotFoundRequestFilter.class), GrSciCollEditorAuthorizationFilter.class);
    return http.build();
  }

  /** GET /dataset/{key}/document and /dataset/metadata/{key}/document. */
  static boolean isMetadataDocumentRequest(HttpServletRequest request) {
    return "GET".equalsIgnoreCase(request.getMethod())
        && METADATA_DOCUMENT_PATTERN.matcher(RequestPathUtils.normalizedPath(request)).matches();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new RegistryPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    // CorsFilter only applies this if the origin header is present in the request
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type"));
    configuration.setAllowedOrigins(Collections.singletonList("*"));
    configuration.setAllowedMethods(
        Arrays.asList("HEAD", "GET", "POST", "DELETE", "PUT", "OPTIONS"));
    configuration.setExposedHeaders(
        Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Methods",
            "Access-Control-Allow-Headers"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
