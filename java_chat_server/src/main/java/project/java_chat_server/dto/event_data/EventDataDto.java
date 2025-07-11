package project.java_chat_server.dto.event_data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import project.java_chat_server.common.EventType;

@RequiredArgsConstructor
@Getter
public class EventDataDto {
    private final EventType type;
}

