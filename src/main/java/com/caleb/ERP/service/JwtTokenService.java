package com.caleb.ERP.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtTokenService {

    private static final String SECRET_KEY = "48A5360D50EFAE9A612284859CDD1D8F773EDFBEDF89D5B7C83F294392946455";
    private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1 hour

    // Generate access token with role and employeeId claims
    public String generateAccessToken(String employeeEmail, String employeeRole, String employeeId) {
        return Jwts.builder()
                .setSubject(employeeEmail)
                .claim("role", employeeRole) // Store the role without "ROLE_" prefix
                .claim("employeeId", employeeId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // Validate access token
    public boolean validateToken(String token) {
        return extractAllClaims(token).getExpiration().after(new Date());
    }

    // Extract all claims from the token
    public Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    // Extract the employeeId from the token
    public String extractEmployeeId(String token) {
        return extractAllClaims(token).get("employeeId", String.class);
    }
}