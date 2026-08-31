package com.elgris.usersapi.security;

import com.elgris.usersapi.configuration.OperationalProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final byte[] jwtSecret;
    private final ObjectMapper objectMapper;
    private final OperationalProperties operationalProperties;

    public JwtAuthenticationFilter(
            @Value("${jwt.secret}") String jwtSecret,
            ObjectMapper objectMapper,
            OperationalProperties operationalProperties) {
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
        this.operationalProperties = operationalProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (isPublicPath(request) || "OPTIONS".equals(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
            return;
        }

        try {
            request.setAttribute("claims", verify(authHeader.substring(7)));
            chain.doFilter(request, response);
        } catch (IllegalArgumentException | GeneralSecurityException | IOException exception) {
            String message = operationalProperties.features().verboseSecurityErrors()
                    ? "Invalid token: " + exception.getMessage()
                    : "Invalid token";
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
        }
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/metrics".equals(path)
                || "/prometheus".equals(path)
                || "/health".equals(path)
                || path.startsWith("/health/")
                || path.startsWith("/actuator");
    }

    private Map<String, Object> verify(String token) throws GeneralSecurityException, IOException {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT must contain three segments");
        }

        Base64.Decoder decoder = Base64.getUrlDecoder();
        Map<String, Object> header = objectMapper.readValue(decoder.decode(parts[0]), MAP_TYPE);
        if (!"HS256".equals(header.get("alg"))) {
            throw new IllegalArgumentException("Only HS256 tokens are accepted");
        }

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret, "HmacSHA256"));
        byte[] expected = mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        byte[] actual = decoder.decode(parts[2]);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new IllegalArgumentException("JWT signature does not match");
        }

        Map<String, Object> claims = objectMapper.readValue(decoder.decode(parts[1]), MAP_TYPE);
        Object expiresAt = claims.get("exp");
        if (expiresAt instanceof Number number && Instant.now().getEpochSecond() >= number.longValue()) {
            throw new IllegalArgumentException("JWT has expired");
        }
        return claims;
    }
}
