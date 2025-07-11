package project.java_chat_server.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import project.java_chat_server.common.MessageType;
import project.java_chat_server.dto.ChatMessageDto;
import project.java_chat_server.service.ChatServerService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatServerService chatServerService;

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody ChatMessageDto dto) {
        chatServerService.broadcastMessage(dto.getMessage(), MessageType.MSG_TYPE_SERVER_NOTICE, -1);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send/{clientFd}")
    public ResponseEntity<Void> send(@PathVariable("clientFd") int clientFd, @RequestBody ChatMessageDto dto) {
        chatServerService.sendMessageToClient(dto.getMessage(), MessageType.MSG_TYPE_SERVER_NOTICE, clientFd);
        return ResponseEntity.ok().build();
    }
}
