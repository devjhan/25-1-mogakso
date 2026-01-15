package project.java_chat_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceEdgeTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void testLogin_InvalidNickname_Null() {
        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, null);
        });
    }

    @Test
    void testLogin_InvalidNickname_TooShort() {
        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "ab");  // 2 characters, minimum is 3
        });
    }

    @Test
    void testLogin_InvalidNickname_TooLong() {
        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "a".repeat(16));  // 16 characters, maximum is 15
        });
    }

    @Test
    void testLogin_InvalidNickname_InvalidCharacters() {
        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "user-name");  // contains hyphen
        });

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "user_name");  // contains underscore
        });

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "user name");  // contains space
        });

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "user@name");  // contains special character
        });
    }

    @Test
    void testLogin_ValidNickname_MinimumLength() throws UserService.UserLoginException {
        assertDoesNotThrow(() -> {
            userService.login(1, "abc");  // exactly 3 characters
        });
    }

    @Test
    void testLogin_ValidNickname_MaximumLength() throws UserService.UserLoginException {
        assertDoesNotThrow(() -> {
            userService.login(1, "a".repeat(15));  // exactly 15 characters
        });
    }

    @Test
    void testLogin_ValidNickname_Alphanumeric() throws UserService.UserLoginException {
        assertDoesNotThrow(() -> {
            userService.login(1, "user123");
        });

        assertDoesNotThrow(() -> {
            userService.login(2, "USER123");
        });

        assertDoesNotThrow(() -> {
            userService.login(3, "User123");
        });

        assertDoesNotThrow(() -> {
            userService.login(4, "123abc");
        });
    }

    @Test
    void testLogin_NegativeClientId() throws UserService.UserLoginException {
        // Negative client ID should be allowed (validation is done at higher level)
        assertDoesNotThrow(() -> {
            userService.login(-1, "testuser");
        });
        assertTrue(userService.isLoggedIn(-1));
    }

    @Test
    void testLogin_ZeroClientId() throws UserService.UserLoginException {
        assertDoesNotThrow(() -> {
            userService.login(0, "testuser");
        });
        assertTrue(userService.isLoggedIn(0));
    }

    @Test
    void testLogin_MaxClientId() throws UserService.UserLoginException {
        int maxClientId = Integer.MAX_VALUE;
        assertDoesNotThrow(() -> {
            userService.login(maxClientId, "testuser");
        });
        assertTrue(userService.isLoggedIn(maxClientId));
    }

    @Test
    void testMultipleLogins_SameClientDifferentNicknames() throws UserService.UserLoginException {
        int clientId = 1;
        userService.login(clientId, "first");

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(clientId, "second");
        });
    }

    @Test
    void testMultipleLogins_DifferentClientsSameNickname() throws UserService.UserLoginException {
        userService.login(1, "samenick");

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(2, "samenick");
        });
    }

    @Test
    void testLogout_AfterLogin() throws UserService.UserLoginException {
        int clientId = 1;
        userService.login(clientId, "testuser");
        userService.logout(clientId);

        // Should be able to login again with same nickname
        assertDoesNotThrow(() -> {
            userService.login(clientId, "testuser");
        });
    }

    @Test
    void testLogout_AfterLogin_DifferentNickname() throws UserService.UserLoginException {
        int clientId = 1;
        userService.login(clientId, "first");
        userService.logout(clientId);

        // Should be able to login with different nickname
        assertDoesNotThrow(() -> {
            userService.login(clientId, "second");
        });
    }

    @Test
    void testConcurrentOperations() throws UserService.UserLoginException {
        // Login multiple users
        for (int i = 1; i <= 100; i++) {
            userService.login(i, "user" + i);
        }

        assertEquals(100, userService.getUserIds().size());

        // Logout some users
        for (int i = 1; i <= 50; i++) {
            userService.logout(i);
        }

        assertEquals(50, userService.getUserIds().size());

        // Login new users
        for (int i = 101; i <= 150; i++) {
            userService.login(i, "user" + i);
        }

        assertEquals(100, userService.getUserIds().size());
    }
}
