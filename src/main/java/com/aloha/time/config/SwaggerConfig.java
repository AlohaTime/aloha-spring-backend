package com.aloha.time.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "알로하 타임 API 명세서",
                version = "v1"))
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi customTestOpenApi() {
        String[] paths = {"/test/**"};

        return GroupedOpenApi
                .builder()
                .group("테스트 API")
                .pathsToMatch(paths)
                .build();
    }

}