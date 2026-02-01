//package com.ebingo.backend.system.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsWebFilter;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//
//import java.util.List;
//
//@Configuration
//public class CorsGlobalConfig {
//
//    @Value("${app.cors.allowed-origins:http://localhost:3000}")
//    private List<String> allowedOrigins;
//
//    @Bean
//    public CorsWebFilter corsWebFilter() {
//        CorsConfiguration cors = new CorsConfiguration();
//
//        cors.setAllowedOrigins(allowedOrigins);
//        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        cors.setAllowedHeaders(List.of("Content-Type", "Authorization"));
//        cors.setAllowCredentials(true);
//        cors.setMaxAge(3600L); // cache preflight response for 1 hour
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", cors);
//
//        return new CorsWebFilter(source);
//    }
//}


package com.ebingo.backend.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class CorsGlobalConfig implements WebFluxConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/ws/**")
                .allowedOrigins("*") // Allow all origins for dev
                .allowedMethods("GET", "POST")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
