package com.tathanhloc.faceattendance.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        log.debug("🔍 JWT Filter processing: {} {}", method, requestPath);

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                log.debug("🎫 JWT token found: {}...", jwt.substring(0, Math.min(jwt.length(), 20)));

                if (tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.getUsernameFromToken(jwt);

                    if (username != null) {
                        log.debug("✅ Valid JWT token for user: {}", username);

                        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities()
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("🔐 Authentication set for user: {} with authorities: {}",
                                username, userDetails.getAuthorities());
                    } else {
                        log.warn("❌ Username is null from JWT token");
                    }
                } else {
                    log.warn("❌ Invalid JWT token");
                }
            } else {
                log.debug("🚫 No JWT token found in request");
            }
        } catch (Exception ex) {
            log.error("❌ Could not set user authentication in security context for request: {} - Error: {}",
                    requestPath, ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        // Log authentication state before proceeding
        boolean isAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal());

        log.debug("🎭 Authentication status for {}: {} (Principal: {})",
                requestPath,
                isAuthenticated,
                SecurityContextHolder.getContext().getAuthentication() != null ?
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal().getClass().getSimpleName() : "null");

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Skip JWT filtering for public paths
        boolean shouldSkip = path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/images/") ||
                path.equals("/favicon.ico") ||
                path.equals("/error") ||
                path.equals("/") ||
                path.equals("/index") ||
                path.equals("/index.html") ||
                path.equals("/login") ||
                path.startsWith("/api/auth/");

        if (shouldSkip) {
            log.debug("⏭️ Skipping JWT filter for public path: {}", path);
        }

        return shouldSkip;
    }
}