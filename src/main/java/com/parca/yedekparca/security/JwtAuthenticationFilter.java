
package com.parca.yedekparca.security;

import com.parca.yedekparca.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Authorization header yoksa veya Bearer ile başlamıyorsa devam et.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token içerisindeki Bearer ifadesini kaldır.
        String jwt = authHeader.substring(7);

        // Token içerisinden kullanıcı e-postasını al.
        String email;
        try {
            email = jwtService.extractEmail(jwt);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            log.debug("Rejected invalid bearer token: {}", ex.getClass().getSimpleName());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz veya süresi dolmuş oturum.");
            return;
        }

        // Kullanıcı henüz doğrulanmamışsa kimlik doğrulamasını yap.
        if (email != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(email);
            } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz oturum.");
                return;
            }

            // JWT geçerliyse kullanıcıyı SecurityContext'e ekle.
            if (!userDetails.isEnabled() || !jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                log.debug("Rejected bearer token for disabled or invalid account");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Geçersiz veya süresi dolmuş oturum.");
                return;
            }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
