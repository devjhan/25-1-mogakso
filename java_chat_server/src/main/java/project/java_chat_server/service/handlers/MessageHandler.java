package project.java_chat_server.service.handlers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.dto.common.ErrorResponse;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class MessageHandler {
    protected final ObjectMapper objectMapper;

    abstract public HandlerResult handle(ClientInfo client, byte[] payload);
    abstract public MessageType getMessageType();

    protected HandlerResult createErrorResponse(String errorCode, String message) {
        ErrorResponse error = new ErrorResponse(errorCode, message);
        return HandlerResult.response(MessageType.MSG_TYPE_ERROR_RESPONSE, error);
    }
}
