package com.elgris.usersapi.api;

import com.elgris.usersapi.models.User;
import com.elgris.usersapi.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the /users endpoints (spec 006 / T012). Pure Mockito, no Spring
 * context: exercises the listing, the JWT-claim ownership check, and the
 * missing-claims guard.
 */
@RunWith(MockitoJUnitRunner.class)
public class UsersControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UsersController controller;

    private User user(String username) {
        User u = new User();
        u.setUsername(username);
        return u;
    }

    @Test
    public void getUsersReturnsAllUsersFromRepository() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(user("alice"), user("bob")));

        List<User> result = controller.getUsers();

        assertEquals(2, result.size());
        assertEquals("alice", result.get(0).getUsername());
    }

    @Test
    public void getUserReturnsUserWhenTokenUsernameMatchesCaseInsensitive() {
        Claims claims = Jwts.claims();
        claims.put("username", "Alice");
        when(request.getAttribute("claims")).thenReturn(claims);
        when(userRepository.findOneByUsername("alice")).thenReturn(user("alice"));

        User result = controller.getUser(request, "alice");

        assertEquals("alice", result.getUsername());
    }

    @Test(expected = AccessDeniedException.class)
    public void getUserDeniesWhenUsernameDoesNotMatchClaims() {
        Claims claims = Jwts.claims();
        claims.put("username", "bob");
        when(request.getAttribute("claims")).thenReturn(claims);

        controller.getUser(request, "alice");
    }

    @Test(expected = RuntimeException.class)
    public void getUserThrowsWhenClaimsAttributeMissing() {
        when(request.getAttribute("claims")).thenReturn(null);

        controller.getUser(request, "alice");
    }
}
