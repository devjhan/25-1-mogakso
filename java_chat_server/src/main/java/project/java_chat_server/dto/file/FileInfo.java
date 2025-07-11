package project.java_chat_server.dto.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileInfo {
    @JsonProperty("filename")
    private String filename;

    @JsonProperty("filesize")
    private long filesize;
}
