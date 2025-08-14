package project.java_chat_server.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.java_chat_server.dto.user.UserLeaveBroadcast;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.service.handlers.*;
import project.java_chat_server.wrapper_library.ChatServer;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.wrapper_library.enums.MessageType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChatService {
    private static final Set<MessageType> VALID_CLIENT_MESSAGE_TYPES = EnumSet.of(
            MessageType.MSG_TYPE_USER_LOGIN_REQUEST,
            MessageType.MSG_TYPE_CHAT_TEXT,
            MessageType.MSG_TYPE_FILE_INFO,
            MessageType.MSG_TYPE_FILE_CHUNK,
            MessageType.MSG_TYPE_FILE_END,
            MessageType.MSG_TYPE_FILE_REQUEST
    );
    private final Map<MessageType, MessageHandler> messageHandlers;
    private final UserService userService;
    private final ChatServer chatServer;
    private final ObjectMapper objectMapper;

    public ChatService(ChatServer chatServer, List<MessageHandler> handlers, UserService userService, ObjectMapper objectMapper) {
        this.chatServer = chatServer;
        this.messageHandlers = handlers.stream().collect(Collectors.toUnmodifiableMap(MessageHandler::getMessageType, Function.identity()));
        this.userService = userService;
        this.objectMapper = objectMapper;
        log.info("{}개의 메시지 핸들러가 등록되었습니다: {}", messageHandlers.size(), messageHandlers.keySet());
    }

    public void handleClientConnected(ClientInfo client) {
        log.info("새로운 클라이언트 연결 수립: id={}, ip={}", client.socketFd, client.ipAddr);
    }

    public void handleClientDisconnected(ClientInfo client) {
        String nickname = userService.logout(client.socketFd);
        log.info("클라이언트 연결 종료: id={}, nickname={}", client.socketFd, nickname);

        UserLeaveBroadcast leaveNotice = new UserLeaveBroadcast(nickname);

        try {
            byte[] payload = objectMapper.writeValueAsBytes(leaveNotice);
            chatServer.broadcast(MessageType.MSG_TYPE_USER_LEAVE_NOTICE, payload, -1);
        } catch (JsonProcessingException e) {
            log.error("사용자 퇴장 공지 직렬화 실패: {}", nickname, e);
        } catch (IOException e) {
            log.error("사용자 퇴장 공지 방송 실패: {}", nickname, e);
        }
    }

    public void handleMessageReceived(ClientInfo client, int msgTypeInt, byte[] payload) {
        MessageType msgType = MessageType.fromValue(msgTypeInt);

        if (!isValidMessage(msgType)) {
            handleInvalidMessage(client, msgType);
            return;
        }

        MessageHandler handler = messageHandlers.get(msgType);

        if (handler == null) {
            log.warn("유효한 메시지 타입이지만, 처리할 핸들러가 없습니다: {}", msgType);
            return;
        }
        HandlerResult result = handler.handle(client, payload);
        executeHandlerResult(result, client.socketFd);
    }

    private void handleInvalidMessage(ClientInfo client, MessageType msgType) {
        log.warn("프로토콜 위반 감지. 클라이언트(id:{}, ip:{})가 유효하지 않은 메시지 타입({})을 전송했습니다.", client.socketFd, client.ipAddr, msgType);
        String errorMessage = String.format("Error: Invalid message type (%s) sent from client.", msgType.name());

        try {
            chatServer.sendToClient(client.socketFd, MessageType.MSG_TYPE_ERROR_RESPONSE, errorMessage.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("클라이언트(id:{})에게 에러 응답 전송 실패.", client.socketFd, e);
        }
    }

    private boolean isValidMessage(MessageType msgType) {
        return VALID_CLIENT_MESSAGE_TYPES.contains(msgType);
    }

    private void executeHandlerResult(HandlerResult result, int senderId) {
        result.getDirectResponse().ifPresent(response -> {
            Object dto = response.payload();

            try {
                byte[] payloadBytes = objectMapper.writeValueAsBytes(dto);
                chatServer.sendToClient(senderId, response.type(), payloadBytes);
            } catch (IOException e) {
                log.error("클라이언트(id:{})에게 직접 응답 전송 실패", senderId, e);
            }
        });

        result.getBroadcast().ifPresent(broadcast -> {
            Object dto = broadcast.payload();

            try {
                byte[] payloadBytes;

                if (broadcast.type() == MessageType.MSG_TYPE_FILE_CHUNK) {
                    payloadBytes = (byte[]) dto;
                } else {
                    payloadBytes = objectMapper.writeValueAsBytes(dto);
                }
                chatServer.broadcast(broadcast.type(), payloadBytes, senderId);
            } catch (IOException e) {
                log.error("메시지 방송 실패", e);
            }
        });
    }
}
