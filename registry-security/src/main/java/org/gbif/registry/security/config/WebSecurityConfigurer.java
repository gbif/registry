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
import org.gbif.registry.security.AfterAppIdentityFilterLogger;
import org.gbif.registry.security.AfterAuthPreCheckCreationRequestFilterLogger;
import org.gbif.registry.security.AfterEditorAuthorizationFilterLogger;
import org.gbif.registry.security.AfterGrSciCollEditorAuthorizationFilterLogger;
import org.gbif.registry.security.AfterHttpServletRequestWrapperFilterLogger;
import org.gbif.registry.security.AfterIdentityFilterLogger;
import org.gbif.registry.security.AfterJwtRequestFilterLogger;
import org.gbif.registry.security.AfterLegacyAuthorizationFilterLogger;
import org.gbif.registry.security.AfterRequestHeaderParamUpdateFilterLogger;
import org.gbif.registry.security.AfterResourceNotFoundRequestFilterLogger;
import org.gbif.registry.security.EditorAuthorizationFilter;
import org.gbif.registry.security.LegacyAuthorizationFilter;
import org.gbif.registry.security.ResourceNotFoundRequestFilter;
import org.gbif.registry.security.grscicoll.GrSciCollEditorAuthorizationFilter;
import org.gbif.registry.security.jwt.JwtRequestFilter;
import org.gbif.registry.security.precheck.AuthPreCheckCreationRequestFilter;
import org.gbif.ws.server.filter.AppIdentityFilter;
import org.gbif.ws.server.filter.HttpServletRequestWrapperFilter;
import org.gbif.ws.server.filter.IdentityFilter;
import org.gbif.ws.server.filter.RequestHeaderParamUpdateFilter;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfigurer {

  private final ApplicationContext context;

  private final UserDetailsService userDetailsService;

  public WebSecurityConfigurer(
      @Qualifier("registryUserDetailsService") UserDetailsService userDetailsService,
      ApplicationContext context) {
    this.userDetailsService = userDetailsService;
    this.context = context;
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

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .httpBasic(AbstractHttpConfigurer::disable)
      .csrf(AbstractHttpConfigurer::disable)
      .cors(cors -> cors.configurationSource(corsConfigurationSource()))
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(authz -> authz
        .anyRequest().authenticated()
      );

    // Add filters with logging after each one - chain them sequentially
    http.addFilterAfter(context.getBean("httpServletRequestWrapperFilter", HttpServletRequestWrapperFilter.class), CsrfFilter.class)
        .addFilterAfter(new AfterHttpServletRequestWrapperFilterLogger(), HttpServletRequestWrapperFilter.class)
        .addFilterAfter(context.getBean("requestHeaderParamUpdateFilter", RequestHeaderParamUpdateFilter.class), AfterHttpServletRequestWrapperFilterLogger.class)
        .addFilterAfter(new AfterRequestHeaderParamUpdateFilterLogger(), RequestHeaderParamUpdateFilter.class)
        .addFilterAfter(context.getBean(IdentityFilter.class), AfterRequestHeaderParamUpdateFilterLogger.class)
        .addFilterAfter(new AfterIdentityFilterLogger(), IdentityFilter.class)
        .addFilterAfter(context.getBean("legacyAuthorizationFilter", LegacyAuthorizationFilter.class), AfterIdentityFilterLogger.class)
        .addFilterAfter(new AfterLegacyAuthorizationFilterLogger(), LegacyAuthorizationFilter.class)
        .addFilterAfter(context.getBean("appIdentityFilter", AppIdentityFilter.class), AfterLegacyAuthorizationFilterLogger.class)
        .addFilterAfter(new AfterAppIdentityFilterLogger(), AppIdentityFilter.class)
        .addFilterAfter(context.getBean("jwtRequestFilter", JwtRequestFilter.class), AfterAppIdentityFilterLogger.class)
        .addFilterAfter(new AfterJwtRequestFilterLogger(), JwtRequestFilter.class)
        .addFilterAfter(context.getBean("authPreCheckCreationRequestFilter", AuthPreCheckCreationRequestFilter.class), AfterJwtRequestFilterLogger.class)
        .addFilterAfter(new AfterAuthPreCheckCreationRequestFilterLogger(), AuthPreCheckCreationRequestFilter.class)
        .addFilterAfter(context.getBean("editorAuthorizationFilter", EditorAuthorizationFilter.class), AfterAuthPreCheckCreationRequestFilterLogger.class)
        .addFilterAfter(new AfterEditorAuthorizationFilterLogger(), EditorAuthorizationFilter.class)
        .addFilterAfter(context.getBean("grSciCollEditorAuthorizationFilter", GrSciCollEditorAuthorizationFilter.class), AfterEditorAuthorizationFilterLogger.class)
        .addFilterAfter(new AfterGrSciCollEditorAuthorizationFilterLogger(), GrSciCollEditorAuthorizationFilter.class)
        .addFilterAfter(context.getBean("resourceNotFoundRequestFilter", ResourceNotFoundRequestFilter.class), AfterGrSciCollEditorAuthorizationFilterLogger.class)
        .addFilterAfter(new AfterResourceNotFoundRequestFilterLogger(), ResourceNotFoundRequestFilter.class);

    return http.build();
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
