package com.chatapp.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String requestURI = request.getRequestURI();
        log.debug("JWT Filter: Processing request to {}", requestURI);

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("JWT Filter: No Bearer token found for {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwt = authHeader.substring(7);
            log.debug("JWT Filter: Extracted JWT token (length: {})", jwt.length());

            username = jwtTokenProvider.extractUsername(jwt);
            log.debug("JWT Filter: Extracted username: {}", username);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                log.debug("JWT Filter: Loaded UserDetails for {}, Authorities: {}", username, userDetails.getAuthorities());

                if (jwtTokenProvider.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT Filter: Authentication set for user: {} with authorities: {}", username, userDetails.getAuthorities());
                } else {
                    log.warn("JWT Filter: Invalid token for user: {}", username);
                }
            } else if (username == null) {
                log.warn("JWT Filter: Could not extract username from token");
            } else {
                log.debug("JWT Filter: Authentication already exists for user: {}", username);
            }
        } catch (Exception e) {
            log.error("JWT Filter: Error processing JWT token for {}: {}", requestURI, e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
