package project.java_chat_server.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class UserService {
    private final Map<Integer, String> loggedInUsers = new ConcurrentHashMap<>();

    public void login(int clientId, String nickname) throws UserLoginException {
        if (isLoggedIn(clientId)) {
            throw new UserLoginException("이미 로그인된 상태입니다.");
        }

        if (!isNicknameValid(nickname)) {
            throw new UserLoginException("닉네임은 3~15자의 영문/숫자만 가능합니다.");
        }

        if (isNicknameTaken(nickname)) {
            throw new UserLoginException("이미 사용 중인 닉네임입니다.");
        }

        loggedInUsers.put(clientId, nickname);
        log.info("사용자 등록 완료: 클라이언트(id:{}) -> 닉네임 '{}'", clientId, nickname);
    }

    public String logout(int clientId) {
        final String nickname = loggedInUsers.remove(clientId);
        if (nickname != null) {
            log.info("사용자 제거 완료: 닉네임 '{}' (id:{})", nickname, clientId);
        }
        return nickname;
    }

    public Optional<String> getNickname(int clientId) {
        return Optional.ofNullable(loggedInUsers.get(clientId));
    }

    public boolean isLoggedIn(int clientId) {
        return loggedInUsers.containsKey(clientId);
    }

    private boolean isNicknameTaken(String nickname) {
        return loggedInUsers.containsValue(nickname);
    }

    private boolean isNicknameValid(String nickname) {
        if (nickname == null || nickname.length() < 3 || nickname.length() > 15) {
            return false;
        }
        return nickname.matches("^[a-zA-Z0-9]+$");
    }

    public Set<Integer> getAllLoggedInClientIds() {
        return loggedInUsers.keySet();
    }

    public static class UserLoginException extends Exception {
        public UserLoginException(String message) {
            super(message);
        }
    }
}
