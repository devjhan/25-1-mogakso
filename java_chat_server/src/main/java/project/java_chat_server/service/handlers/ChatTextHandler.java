package project.java_chat_server.service.handlers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.dto.chat.ChatTextBroadcast;
import project.java_chat_server.dto.chat.ChatTextRequest;
import project.java_chat_server.service.UserService;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import java.io.IOException;

@Slf4j
@Component
public class ChatTextHandler extends MessageHandler{
    private final UserService userService;

    public ChatTextHandler(ObjectMapper objectMapper, UserService userService) {
        super(objectMapper);
        this.userService = userService;
    }

    @Override
    public HandlerResult handle(ClientInfo client, byte[] payload) {
        try {
            String senderNickname = userService.getNickname(client.socketFd)
                    .orElseThrow(() -> new IllegalStateException("Authentication required. Client not logged in."));

            ChatTextRequest request = objectMapper.readValue(payload, ChatTextRequest.class);
            String messageContent = request.message();

            if (messageContent == null || messageContent.isBlank()) {
                log.trace("{} : client {}({}) sent an empty message. Ignored.", this.getClass().getSimpleName(), client.socketFd, senderNickname);
                return HandlerResult.empty();
            }

            ChatTextBroadcast broadcastDto = new ChatTextBroadcast(senderNickname, messageContent);
            return HandlerResult.broadcast(MessageType.MSG_TYPE_CHAT_TEXT, broadcastDto);
        } catch (IllegalStateException e) {
            log.warn("{} : chat message rejected for unauthenticated client {}. details: {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("AUTH_REQUIRED", "채팅을 하려면 먼저 로그인해야 합니다.");
        } catch (IOException e) {
            log.error("{} : failed to parse chat request for client {}. details: {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("INVALID_REQUEST_FORMAT", "요청 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            log.error("{} : unknown error while processing chat for client {}. details: {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage(), e);
            return super.createErrorResponse("UNKNOWN_ERROR", "알 수 없는 오류가 발생했습니다.");
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.MSG_TYPE_CHAT_TEXT;
    }
}
