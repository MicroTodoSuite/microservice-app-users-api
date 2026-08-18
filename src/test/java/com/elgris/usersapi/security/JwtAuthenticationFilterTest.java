package com.elgris.usersapi.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the JWT authentication filter (spec 006 / T012): observability
 * paths bypass auth, a validly-signed bearer token yields claims, and a missing
 * or badly-signed token is rejected.
 */
public class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-value";

    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @Before
    public void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", SECRET);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    private String tokenSignedWith(String secret) {
        return Jwts.builder()
                .claim("username", "alice")
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

    @Test
    public void metricsPathBypassesAuthentication() throws Exception {
        when(request.getRequestURI()).thenReturn("/metrics");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).setAttribute(eq("claims"), any());
    }

    @Test
    public void validBearerTokenSetsClaimsAndContinues() throws Exception {
        when(request.getRequestURI()).thenReturn("/users/alice");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("authorization")).thenReturn("Bearer " + tokenSignedWith(SECRET));

        filter.doFilter(request, response, chain);

        verify(request).setAttribute(eq("claims"), any());
        verify(chain).doFilter(request, response);
    }

    @Test(expected = ServletException.class)
    public void missingAuthorizationHeaderIsRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/users");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("authorization")).thenReturn(null);

        filter.doFilter(request, response, chain);
    }

    @Test(expected = ServletException.class)
    public void invalidTokenSignatureIsRejected() throws Exception {
        when(request.getRequestURI()).thenReturn("/users/alice");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("authorization")).thenReturn("Bearer " + tokenSignedWith("a-different-secret"));

        filter.doFilter(request, response, chain);
    }
}
