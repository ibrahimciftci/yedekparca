package com.parca.yedekparca.config;

import com.parca.yedekparca.security.JwtAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, Environment environment) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // Session kullanmayacağız, JWT ile kimlik doğrulayacağız.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(request -> isDemoImageGet(request)).permitAll()
                        // Kayıt ve giriş herkese açık.
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        // Ürün kataloğu herkese açıktır; katalog görünürlüğü servis sorgusunda kısıtlanır.
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                        // Keep the directory route public without exposing /api/sellers/me.
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/catalog/products", "/api/catalog/products/**", "/api/marketplace/catalog",
                                "/api/catalog/categories", "/api/catalog/brands",
                                "/api/sellers", "/api/sellers/*/profile", "/api/sellers/*/offers").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/locations/**").permitAll()

                        // Admin endpoint'lerine sadece ADMIN rolü erişebilir.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Ürün yönetimi endpoint'leri satıcı rolü gerektirir.
                        .requestMatchers("/api/seller/products/**").hasRole("SELLER")
                        .requestMatchers("/api/seller/**").hasRole("SELLER")

                        // Diğer endpoint'ler giriş yapmış kullanıcılara açık.
                        .anyRequest().authenticated()
                )

                // JWT filtresini UsernamePasswordAuthenticationFilter öncesine ekle.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private boolean isDemoImageGet(jakarta.servlet.http.HttpServletRequest request) {
        boolean demoProfile = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev-demo"))
                && !environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"))
                && !environment.acceptsProfiles(org.springframework.core.env.Profiles.of("production"));
        if (!demoProfile || !org.springframework.http.HttpMethod.GET.matches(request.getMethod())) return false;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return com.parca.yedekparca.controller.DemoImageController.isAllowedPublicGetPath(path);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept")
        );

        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
