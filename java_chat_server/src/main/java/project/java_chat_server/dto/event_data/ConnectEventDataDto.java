package project.java_chat_server.dto.event_data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import project.java_chat_server.common.EventType;

@Getter
public class ConnectEventDataDto extends EventDataDto {
    private final int id;
    private final String ip;

    public ConnectEventDataDto(EventType type, int id, String ip) {
        super(type);
        this.id = id;
        this.ip = ip;
    }
}
