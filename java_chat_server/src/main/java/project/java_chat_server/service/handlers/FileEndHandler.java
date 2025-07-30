package project.java_chat_server.service.handlers;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.java_chat_server.dto.common.SystemNoticeBroadcast;
import project.java_chat_server.service.model.HandlerResult;
import project.java_chat_server.dto.file.FileEndBroadcast;
import project.java_chat_server.dto.file.FileEndRequest;
import project.java_chat_server.service.FileTransferService;
import project.java_chat_server.service.UserService;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

import java.io.IOException;

@Slf4j
@Component
public class FileEndHandler extends MessageHandler{
    private final FileTransferService fileTransferService;
    private final UserService userService;

    public FileEndHandler(FileTransferService fileTransferService, UserService userService, ObjectMapper objectMapper) {
        super(objectMapper);
        this.fileTransferService = fileTransferService;
        this.userService = userService;
    }

    @Override
    public HandlerResult handle(ClientInfo client, byte[] payload) {
        String senderNickname = null;
        FileEndRequest request = null;

        try {
            senderNickname = userService.getNickname(client.socketFd).orElseThrow(
                    () -> new IllegalStateException("Authentication required. Client not logged in."));

            request = objectMapper.readValue(payload, FileEndRequest.class);

            fileTransferService.endFileTransfer(client.socketFd, request.checksum());
            FileEndBroadcast broadcast = new FileEndBroadcast(senderNickname, request.filename(), "COMPLETED");
            log.info("{} : client {} succeeded to transfer file '{}'", this.getClass().getSimpleName(), client.socketFd, request.filename());
            SystemNoticeBroadcast noticeBroadcast = new SystemNoticeBroadcast(String.format("[SYSTEM] %s 님이 %s 을(를) 전송했습니다.", userService.getNickname(client.socketFd), request.filename()));
            return HandlerResult.response(MessageType.MSG_TYPE_FILE_END, broadcast).andBroadcast(MessageType.MSG_TYPE_SERVER_NOTICE, noticeBroadcast);

        } catch (IllegalStateException e) {
            log.warn("{} : file transfer end rejected for unauthenticated client {}. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("AUTH_REQUIRED", "파일을 전송하려면 먼저 로그인해야 합니다.");
        } catch (IOException e) {
            log.error("{} : failed to parse file end request for client {}. details : {}", this.getClass().getSimpleName(), client.socketFd, e.getMessage());
            return super.createErrorResponse("INVALID_REQUEST_FORMAT", "요청 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            String filename = (request != null) ? request.filename() : "unknown file";
            log.error("{} : failed to finalize file transfer for client {}. filename: '{}', details: {}", this.getClass().getSimpleName(), client.socketFd, filename, e.getMessage(), e);

            FileEndBroadcast failedBroadcast = new FileEndBroadcast(senderNickname, filename, "FAILED");

            return super.createErrorResponse("FILE_TRANSFER_FAILED", "파일 전송 마무리에 실패했습니다: " + e.getMessage())
                    .andBroadcast(MessageType.MSG_TYPE_ERROR_RESPONSE, failedBroadcast);
        }
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.MSG_TYPE_FILE_END;
    }
}
