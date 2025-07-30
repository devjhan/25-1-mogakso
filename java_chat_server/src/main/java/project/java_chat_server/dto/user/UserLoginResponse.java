package project.java_chat_server.dto.user;
import lombok.Getter;

@Getter
public class UserLoginResponse {
    private final boolean success;
    private final String message;
    private final String nickname;
    private final int clientId;

    private UserLoginResponse(boolean success, String message, String nickname, int clientId) {
        this.success = success;
        this.message = message;
        this.nickname = nickname;
        this.clientId = clientId;
    }

    public static UserLoginResponse onSuccess(String nickname, int clientId) {
        return new UserLoginResponse(true, "success", nickname, clientId);
    }

    public static UserLoginResponse onFailure(String message) {
        return new UserLoginResponse(false, message, null, -1);
    }
}
