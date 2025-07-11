package project.java_chat_server.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public enum MessageType {
    MSG_TYPE_UNKNOWN(0),

    MSG_TYPE_CHAT_TEXT(1),

    MSG_TYPE_FILE_INFO(10),
    MSG_TYPE_FILE_CHUNK(11),
    MSG_TYPE_FILE_END(12),

    MSG_TYPE_USER_LOGIN_REQUEST(100),
    MSG_TYPE_USER_LOGIN_RESPONSE(101),

    MSG_TYPE_USER_JOIN_NOTICE(200),
    MSG_TYPE_USER_LEAVE_NOTICE(201),
    MSG_TYPE_SERVER_NOTICE(202),

    MSG_TYPE_ERROR_RESPONSE(500),

    MSG_TYPE_PING(900),
    MSG_TYPE_PONG(901);

    private final int value;

    private static final Map<Integer, MessageType> valueMap = Stream.of(values()).collect(Collectors.toMap(MessageType::getValue, Function.identity()));

    public static MessageType fromValue(int value) {
        return valueMap.getOrDefault(value, MSG_TYPE_UNKNOWN);
    }
}
