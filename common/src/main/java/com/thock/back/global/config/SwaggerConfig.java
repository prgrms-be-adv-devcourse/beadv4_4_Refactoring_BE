package com.thock.back.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    // 환경 변수로 주입
    @Value("${swagger.server.url:http://localhost:8080}")
    private String swaggerServerUrl;

    @Value("${swagger.server.description:API Gateway}")
    private String swaggerServerDescription;

    @Bean
    public OpenAPI customOpenAPI() {

        // JWT 토큰을 위한 보안 스키마 이름 정의
        final String securitySchemeName = "bearerAuth";

        // 환경별로 다른 URL 사용
        io.swagger.v3.oas.models.servers.Server server = new io.swagger.v3.oas.models.servers.Server();
        server.setUrl(swaggerServerUrl);
        server.setDescription(swaggerServerDescription);

        return new OpenAPI()
                .info(new Info()
                        .title("Thock API Docs")
                        .version("v1.0.0")
                        .description("Thock 프로젝트용 Swagger 문서입니다.")
                )
                .addServersItem(server);
    }
}

    // 드롭다운
    /**
     * 일반 사용자용 API 그룹(인증 필요)
     */
//    @Bean
//    public GroupedOpenApi userApi() {
//        return GroupedOpenApi.builder()
//                .group("user")
//                .displayName("1. User API (Auth Required)")
//                .pathsToMatch("")
//                .build();
//    }

    /**
     * 관리자용 API 그룹(인증 불필요)
     * TODO : 나중에 매번 로그인 하여 토큰 발급 받기 귀찮은 경우 사용
     */
//    @Bean
//    public GroupedOpenApi adminApi() {
//        return GroupedOpenApi.builder()
//                .group("admin")
//                .displayName("2. Admin API (No Auth Required)")
//                .pathsToMatch("/admin-test/**")
//                .addOpenApiCustomizer(openApi -> openApi
//                        .security(Collections.emptyList())) // SecurityRequirement 제거
//                .build();
//    }


