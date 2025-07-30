package project.java_chat_server.dto.chat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatTextRequest(String message) {
    @JsonCreator
    public ChatTextRequest(@JsonProperty("message") String message) {
        this.message = message;
    }

}
