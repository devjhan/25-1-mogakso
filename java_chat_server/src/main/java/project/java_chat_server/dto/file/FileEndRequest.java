package project.java_chat_server.dto.file;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileEndRequest(String filename, String checksum) {
    @JsonCreator
    public FileEndRequest(
            @JsonProperty("filename") String filename,
            @JsonProperty("checksum") String checksum) {
        this.filename = filename;
        this.checksum = checksum;
    }

}
