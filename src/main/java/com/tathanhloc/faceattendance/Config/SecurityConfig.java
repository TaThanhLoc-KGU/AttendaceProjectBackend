package com.tathanhloc.faceattendance.Config;

import com.tathanhloc.faceattendance.Security.CustomUserDetailsService;
import com.tathanhloc.faceattendance.Security.JwtAuthenticationEntryPoint;
import com.tathanhloc.faceattendance.Security.JwtAuthenticationFilter;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

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
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestURI = request.getRequestURI();
                            System.out.println("🔒 Authentication required for: " + requestURI);
                            System.out.println("🔒 User Principal: " + request.getUserPrincipal());
                            System.out.println("🔒 Auth Header: " + request.getHeader("Authorization"));

                            // Nếu là AJAX request, trả về JSON
                            if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With")) ||
                                    request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json")) {
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                            } else {
                                // Nếu là browser request, redirect về login
                                response.sendRedirect("/?error=not_authenticated");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            System.out.println("🚫 Access denied for: " + request.getRequestURI());
                            response.sendRedirect("/?error=access_denied");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public resources - KHÔNG cần authentication
                        .requestMatchers("/", "/index", "/index.html", "/login", "/logout").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // Auth APIs - KHÔNG cần authentication
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/python/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // Protected web pages - CẦN authentication + role
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/lecturer/**").hasRole("GIANGVIEN")
                        .requestMatchers("/student/**").hasRole("SINHVIEN")

                        // Protected APIs - CẦN authentication
                        .requestMatchers("/api/**").authenticated()

                        // All other requests - CẦN authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(List.of("x-auth-token"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}