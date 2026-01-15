package project.java_chat_server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    void testLogin_Success() throws UserService.UserLoginException {
        int clientId = 1;
        String nickname = "testuser";

        assertDoesNotThrow(() -> userService.login(clientId, nickname));
        assertTrue(userService.isLoggedIn(clientId));
        assertEquals(nickname, userService.getNickname(clientId).orElse(null));
    }

    @Test
    void testLogin_MultipleUsers() throws UserService.UserLoginException {
        userService.login(1, "user1");
        userService.login(2, "user2");
        userService.login(3, "user3");

        assertTrue(userService.isLoggedIn(1));
        assertTrue(userService.isLoggedIn(2));
        assertTrue(userService.isLoggedIn(3));
        assertEquals("user1", userService.getNickname(1).orElse(null));
        assertEquals("user2", userService.getNickname(2).orElse(null));
        assertEquals("user3", userService.getNickname(3).orElse(null));
    }

    @Test
    void testLogin_DuplicateNickname() throws UserService.UserLoginException {
        userService.login(1, "testuser");

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(2, "testuser");
        });
    }

    @Test
    void testLogin_DuplicateClientId() throws UserService.UserLoginException {
        userService.login(1, "user1");

        assertThrows(UserService.UserLoginException.class, () -> {
            userService.login(1, "user2");
        });
    }

    @Test
    void testLogout_Success() throws UserService.UserLoginException {
        int clientId = 1;
        String nickname = "testuser";
        userService.login(clientId, nickname);

        String loggedOutNickname = userService.logout(clientId);
        assertEquals(nickname, loggedOutNickname);
        assertFalse(userService.isLoggedIn(clientId));
        assertTrue(userService.getNickname(clientId).isEmpty());
    }

    @Test
    void testLogout_NotLoggedIn() {
        int clientId = 1;
        String nickname = userService.logout(clientId);
        assertNull(nickname);
        assertFalse(userService.isLoggedIn(clientId));
    }

    @Test
    void testGetNickname_LoggedIn() throws UserService.UserLoginException {
        int clientId = 1;
        String nickname = "testuser";
        userService.login(clientId, nickname);

        assertEquals(nickname, userService.getNickname(clientId).orElse(null));
    }

    @Test
    void testGetNickname_NotLoggedIn() {
        int clientId = 1;
        assertTrue(userService.getNickname(clientId).isEmpty());
    }

    @Test
    void testIsLoggedIn_LoggedIn() throws UserService.UserLoginException {
        int clientId = 1;
        userService.login(clientId, "testuser");
        assertTrue(userService.isLoggedIn(clientId));
    }

    @Test
    void testIsLoggedIn_NotLoggedIn() {
        int clientId = 1;
        assertFalse(userService.isLoggedIn(clientId));
    }

    @Test
    void testGetUserIds() throws UserService.UserLoginException {
        userService.login(1, "user1");
        userService.login(2, "user2");
        userService.login(3, "user3");

        Set<Integer> userIds = userService.getUserIds();
        assertEquals(3, userIds.size());
        assertTrue(userIds.contains(1));
        assertTrue(userIds.contains(2));
        assertTrue(userIds.contains(3));
    }

    @Test
    void testGetAllNicknames() throws UserService.UserLoginException {
        userService.login(1, "user1");
        userService.login(2, "user2");
        userService.login(3, "user3");

        Collection<String> nicknames = userService.getAllNicknames();
        assertEquals(3, nicknames.size());
        assertTrue(nicknames.contains("user1"));
        assertTrue(nicknames.contains("user2"));
        assertTrue(nicknames.contains("user3"));
    }

    @Test
    void testLogout_RemovesFromCollections() throws UserService.UserLoginException {
        userService.login(1, "user1");
        userService.login(2, "user2");
        userService.login(3, "user3");

        userService.logout(2);

        Set<Integer> userIds = userService.getUserIds();
        Collection<String> nicknames = userService.getAllNicknames();

        assertEquals(2, userIds.size());
        assertEquals(2, nicknames.size());
        assertFalse(userIds.contains(2));
        assertFalse(nicknames.contains("user2"));
    }
}
