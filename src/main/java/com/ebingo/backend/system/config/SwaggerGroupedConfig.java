package com.ebingo.backend.system.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerGroupedConfig {

    @Bean
    public GroupedOpenApi publicApiGroup() {
        return GroupedOpenApi.builder()
                .group("Public")
                .pathsToMatch("/api/v1/public/**")
//                .addOpenApiCustomizer(globalHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi publicSecuredApiGroup() {
        return GroupedOpenApi.builder()
                .group("Public Secured")
                .pathsToMatch("/api/v1/secured/**")
//                .addOpenApiCustomizer(globalHeaderCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("Admin")
                .pathsToMatch("/api/v1/admin/**")
//                .addOpenApiCustomizer(globalHeaderCustomizer())
                .build();
    }


}
