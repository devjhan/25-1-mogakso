package project.java_chat_server.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
public class UserDto {

    public enum UserState {
        CONNECTED,
        AUTHENTICATED
    }

    private final int userId;
    private final String ip;
    private final long connectionTime;

    @Setter
    private String username;

    @Setter
    private UserState state;
}
