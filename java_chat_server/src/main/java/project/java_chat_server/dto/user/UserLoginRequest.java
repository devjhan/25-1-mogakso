package project.java_chat_server.dto.user;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserLoginRequest(String nickname) {
    @JsonCreator
    public UserLoginRequest(@JsonProperty("nickname") String nickname) {
        this.nickname = nickname;
    }
}
