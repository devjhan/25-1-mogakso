package project.java_chat_server.dto.file;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FileStartRequest(String filename, long filesize) {
    @JsonCreator
    public FileStartRequest(
            @JsonProperty("filename") String filename,
            @JsonProperty("filesize") long filesize) {
        this.filename = filename;
        this.filesize = filesize;
    }
}
