package com.elgris.usersapi.api;

import com.elgris.usersapi.models.User;
import com.elgris.usersapi.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UsersController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsersController.class);
    private final UserRepository userRepository;

    public UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping({"", "/"})
    @Timed(value = "getUser.list.duration", description = "Time taken to get all users")
    public List<User> getUsers() {
        List<User> response = new LinkedList<>();
        userRepository.findAll().forEach(response::add);
        LOGGER.info("User directory listing completed with {} entries", response.size());
        return response;
    }

    @GetMapping("/{username}")
    @Timed(value = "getUser.duration", description = "Time taken to get a user")
    public User getUser(HttpServletRequest request, @PathVariable String username) {
        Object requestAttribute = request.getAttribute("claims");
        if (!(requestAttribute instanceof Map<?, ?> claims)) {
            throw new IllegalStateException("Did not receive required data from JWT token");
        }
        if (!username.equalsIgnoreCase(String.valueOf(claims.get("username")))) {
            throw new AccessDeniedException("No access for requested entity");
        }
        User user = userRepository.findOneByUsername(username);
        LOGGER.info("User lookup completed");
        return user;
    }
}
