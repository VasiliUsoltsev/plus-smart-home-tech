package ru.yandex.practicum.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(Customizer.withDefaults())
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(auth -> auth
                        // Preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Swagger / OpenAPI
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Публичные GET
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()

                        // Пользовательские
                        .pathMatchers(HttpMethod.POST, "/api/orders/**").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/by-email").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/{id}").hasRole("USER")

                        // Административные
                        .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        .pathMatchers("/api/products/**").hasRole("ADMIN")
                        .pathMatchers("/api/categories/**").hasRole("ADMIN")
                        .pathMatchers("/api/inventory/**").hasRole("ADMIN")

                        // Всё остальное — запрещено
                        .anyExchange().denyAll()
                )
                .build();
    }

    @Bean
    ReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails ivan = User.withUsername("ivan")
                .password(passwordEncoder.encode("ivan"))
                .roles("USER")
                .build();

        UserDetails anna = User.withUsername("anna")
                .password(passwordEncoder.encode("anna"))
                .roles("USER", "ADMIN")
                .build();

        return new MapReactiveUserDetailsService(ivan, anna);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}