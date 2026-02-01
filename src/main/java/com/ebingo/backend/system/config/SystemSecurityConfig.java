//package com.ebingo.backend.system.config;
//
//import com.ebingo.backend.common.Constants;
//import com.ebingo.backend.system.converters.ReactiveCustomRoleConverter;
//import com.ebingo.backend.system.exceptions.CustomAccessDeniedHandler;
//import com.ebingo.backend.system.exceptions.CustomAuthenticationEntryPoint;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.web.cors.CorsConfiguration;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//@Configuration
//@EnableReactiveMethodSecurity
//public class SystemSecurityConfig {
//
//    private final CustomAccessDeniedHandler accessDeniedHandler;
//    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
//
//    public SystemSecurityConfig(CustomAccessDeniedHandler accessDeniedHandler,
//                                CustomAuthenticationEntryPoint authenticationEntryPoint) {
//        this.accessDeniedHandler = accessDeniedHandler;
//        this.authenticationEntryPoint = authenticationEntryPoint;
//    }
//
//    @Bean
//    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ReactiveCustomRoleConverter reactiveCustomRoleConverter) {
//
//        http
//                .csrf(ServerHttpSecurity.CsrfSpec::disable)
//                .cors(cors -> cors.configurationSource(request -> {
//                    CorsConfiguration config = new CorsConfiguration();
//                    config.setAllowedOrigins(List.of("http://localhost:3000",
//                            "https://ebingo-frontend-web.vercel.app",
//                            "https://ebingo-frontend-web-git-main-zekarias-semegnew-negeses-projects.vercel.app",
//                            "https://ebingo-frontend-rfsu1ye5j-zekarias-semegnew-negeses-projects.vercel.app"));
//                    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//                    config.setAllowCredentials(true);
//                    config.setAllowedHeaders(Collections.singletonList("*"));
//                    config.setExposedHeaders(List.of("Authorization"));
//                    config.setMaxAge(3600L);
//                    return config;
//                }))
//                .exceptionHandling(exc -> exc
//                        .accessDeniedHandler(accessDeniedHandler)
//                        .authenticationEntryPoint(authenticationEntryPoint)
//                )
//                .authorizeExchange(exchanges -> exchanges
//                        .pathMatchers(Constants.PUBLIC_API_ENDPOINTS_FOR_ACCESS.toArray(new String[0])).permitAll()
//                        .pathMatchers("/api/v1/public/**").permitAll()
//                        .pathMatchers("/ws/**").permitAll()
//                        .anyExchange().authenticated()
//                )
//
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt.jwtAuthenticationConverter(reactiveCustomRoleConverter))
//                );
//
//        return http.build();
//    }
//}
//
