// ===================================================================
// Updated SecurityConfig.java - Merged với Flask support
// ===================================================================

package com.tathanhloc.faceattendance.Config;

import com.tathanhloc.faceattendance.Security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            String role = authentication.getAuthorities().iterator().next().getAuthority();
                            String redirectUrl = switch (role) {
                                case "ROLE_ADMIN" -> "/admin/dashboard";
                                case "ROLE_GIANGVIEN" -> "/lecturer/dashboard";
                                case "ROLE_SINHVIEN" -> "/student/dashboard";
                                default -> "/";
                            };
                            response.sendRedirect(redirectUrl);
                        })
                        .failureHandler((request, response, exception) -> {
                            response.sendRedirect("/?error=login_failed");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/?message=logout_success")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                        .and()
                        .sessionFixation().migrateSession()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/?error=not_authenticated");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendRedirect("/?error=access_denied");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index", "/index.html", "/login", "/logout").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/uploads/**").permitAll()  // Cho phép truy cập tất cả uploads
                        .requestMatchers("/uploads/students/**").permitAll()  // Cụ thể cho students
                        .requestMatchers("/streams/**").permitAll()  // ✅ THÊM DÒNG NÀY - Cho phép truy cập streams
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()
                        .requestMatchers("/api/python/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // ===== EXISTING ROLE-BASED ACCESS =====
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/lecturer/**").hasRole("GIANGVIEN")
                        .requestMatchers("/student/**").hasRole("SINHVIEN")
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/api/stream/**").authenticated()
                        .requestMatchers("/stream/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ===== ALLOW ORIGINS - Bao gồm Flask và existing =====
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "*",                        // Existing - allow all
                "http://localhost:5000",    // Flask default port
                "http://127.0.0.1:5000",   // Flask alternative
                "http://localhost:8080"     // Spring Boot port
        ));

        // ===== ALLOW METHODS =====
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ===== ALLOW HEADERS - Merged existing + Flask requirements =====
        configuration.setAllowedHeaders(Arrays.asList(
                "authorization",
                "content-type",
                "x-auth-token",
                "Content-Type",
                "Authorization",
                "X-Requested-With",
                "Accept"
        ));

        // ===== EXPOSED HEADERS =====
        configuration.setExposedHeaders(Arrays.asList(
                "x-auth-token"
        ));

        // ===== ALLOW CREDENTIALS =====
        configuration.setAllowCredentials(true);

        // ===== REGISTER CONFIGURATIONS =====
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // Apply to all endpoints (existing behavior)
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
