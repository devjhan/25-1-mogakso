package project.java_chat_server.service.handlers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.java_chat_server.dto.user.UserJoinBroadcast;
import project.java_chat_server.dto.user.UserLoginRequest;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.dto.user.UserLoginResponse;
import project.java_chat_server.service.UserService;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import java.io.IOException;

@Slf4j
@Component
public class LoginRequestHandler extends MessageHandler{
    private final UserService userService;

    public LoginRequestHandler(UserService userService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.userService = userService;
    }

    @Override
    public HandlerResult handle(ClientInfo client, byte[] payload) {
        try {
            UserLoginRequest request = objectMapper.readValue(payload, UserLoginRequest.class);
            String requestedNickname = request.nickname().trim();

            if (requestedNickname.isEmpty()) {
                return super.createErrorResponse("INVALID_NICKNAME", "닉네임을 작성하세요.");
            }
            userService.login(client.socketFd, requestedNickname);

            UserLoginResponse response = UserLoginResponse.onSuccess(requestedNickname, client.socketFd);
            UserJoinBroadcast broadcast = new UserJoinBroadcast(requestedNickname);

            return HandlerResult.response(MessageType.MSG_TYPE_USER_LOGIN_RESPONSE, response).andBroadcast(MessageType.MSG_TYPE_USER_JOIN_NOTICE, broadcast);
        } catch (UserService.UserLoginException e) {
            log.warn("{} : client {} failed to login via UserService.login(). details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("LOGIN_FAILED", e.getMessage());
        } catch (IOException e) {
          log.error("{} : client {} nickname is invalid. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
          return super.createErrorResponse("LOGIN_FAILED", e.getMessage());
        } catch (Exception e) {
            log.error("{} : client {} failed to login because of unknown reason. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return HandlerResult.empty();
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.MSG_TYPE_USER_LOGIN_REQUEST;
    }
}
