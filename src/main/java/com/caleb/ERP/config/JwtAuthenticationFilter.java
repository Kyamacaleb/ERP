package com.caleb.ERP.config;

import com.caleb.ERP.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class    JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                // Validate the token
                if (jwtTokenService.validateToken(token)) {
                    // Extract claims from the token
                    Claims claims = jwtTokenService.extractAllClaims(token);
                    String clientId = claims.getSubject(); // Subject contains the email (clientId)
                    String role = claims.get("role", String.class); // Extract the role claim

                    // Create a list of granted authorities using the extracted role
                    List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role)); // Use the role directly

                    // Log the authorities for debugging
                    System.out.println("Authorities: " + authorities);

                    // Build authentication token with authorities
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            clientId, null, authorities);

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set the authentication in the security context
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (ExpiredJwtException e) {
                // Token has expired, send a 401 Unauthorized response
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
                return; // Stop further processing
            }
        }

        filterChain.doFilter(request, response);
    }}