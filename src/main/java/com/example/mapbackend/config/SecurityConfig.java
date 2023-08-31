package com.example.mapbackend.config;

import com.example.mapbackend.config.filter.TokenRemovalFilter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.logging.Logger;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Value("classpath:public.crt")
    RSAPublicKey jwtPublicKey;

    @Value("classpath:private.key")
    RSAPrivateKey jwtPrivateKey;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        Logger.getLogger("SecurityConfig")
            .info("Security config activated.");

        httpSecurity.csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .addFilterBefore(new TokenRemovalFilter(), BearerTokenAuthenticationFilter.class)
            .oauth2ResourceServer(cfg -> {
                cfg.jwt(jwtCfg -> {
                });
            })
            .sessionManagement(cfg -> {
                cfg.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            })
            .exceptionHandling(cfg -> {
                cfg.authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler());
            })
            .authorizeHttpRequests(cfg -> {
                cfg
                    .requestMatchers("/auth/token").permitAll()
                    .requestMatchers("/auth/sign-up").permitAll()
                    .requestMatchers("/").permitAll()
                    .requestMatchers("/download/**").permitAll()
                    .requestMatchers("/api-docs").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/swagger-ui.html").permitAll()
                    .anyRequest().authenticated();
            });

        return httpSecurity.build();
    }

    @Bean
    CorsConfigurationSource corsDisabler() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.applyPermitDefaultValues();
        configuration.setAllowCredentials(false);
        configuration.addAllowedOriginPattern("*");
        configuration.addAllowedHeader("*");
        configuration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();
    }

    @Bean
    JwtEncoder jwtEncoder() {
        JWK whatIsAJWK = new RSAKey.Builder(jwtPublicKey)
            .privateKey(jwtPrivateKey)
            .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(whatIsAJWK)));
    }

    // 费劲
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .addSecurityItem(
                new SecurityRequirement().addList("Bearer Authentication")
            )
            .components(
                new Components()
                    .addSecuritySchemes(
                        "Bearer Authentication",
                        new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .bearerFormat("JWT")
                            .scheme("bearer")
                    )
            );
    }
}
