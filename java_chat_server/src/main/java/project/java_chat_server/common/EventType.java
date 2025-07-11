package project.java_chat_server.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public enum EventType {
    EVENT_TYPE_UNKNOWN(0),
    EVENT_TYPE_CONNECT(1),
    EVENT_TYPE_DISCONNECT(2),
    EVENT_TYPE_MESSAGE(3),
    EVENT_TYPE_ERROR(4);

    private final int value;
    private static final Map<Integer, EventType> valueMap = Stream.of(values()).collect(Collectors.toMap(EventType::getValue, Function.identity()));

    public static EventType fromValue(int value) {
        return valueMap.getOrDefault(value, EVENT_TYPE_UNKNOWN);
    }
}
