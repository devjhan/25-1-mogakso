package project.java_chat_server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.java_chat_server.dto.UserDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class UserService {
    private final Map<Integer, UserDto> onlineUsers = new ConcurrentHashMap<>();

    public UserDto removeOnlineUser(int userId) {
        return onlineUsers.remove(userId);
    }

    public UserDto getOnlineUser(int userId) {
        return onlineUsers.get(userId);
    }

    public void handleNewConnection(int userId, String ip) {
        if (onlineUsers.containsKey(userId)) {
            log.warn("ID {} 에 대한 연결이 이미 존재합니다. 이전 세션을 덮어씁니다.", userId);
        }

        UserDto newUser = new UserDto(userId, ip, System.currentTimeMillis());
        newUser.setState(UserDto.UserState.CONNECTED);
        onlineUsers.put(userId, newUser);
        log.info("새 사용자 임시 등록: {}", newUser);
    }

    public boolean authenticateUser(int clientId, String username) {
        UserDto user = onlineUsers.get(clientId);

        if (user != null && user.getState() == UserDto.UserState.CONNECTED) {
            user.setUsername(username);
            user.setState(UserDto.UserState.AUTHENTICATED);
            log.info("사용자 인증 완료: {}", user);
            return true;
        }
        return false;
    }
}
