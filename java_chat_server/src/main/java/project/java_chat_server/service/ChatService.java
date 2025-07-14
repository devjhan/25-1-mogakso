package project.java_chat_server.service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.java_chat_server.wrapper_library.ChatServer;
import project.java_chat_server.wrapper_library.structure.ClientInfo;
import project.java_chat_server.wrapper_library.enums.MessageType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
@Slf4j
public class ChatService {
    private final Map<Integer, String> connectedClients = new ConcurrentHashMap<>();
    private final ChatServer chatServer;

    public ChatService(ChatServer chatServer) {
        this.chatServer = chatServer;
    }

    public void handleClientConnected(ChatServer chatServer, ClientInfo client) {
        String nickname = "user" + client.socketFd;
        connectedClients.put(client.socketFd, nickname);

        log.info("[{}] 님이 접속했습니다. (IP: {})", nickname, client.ipAddr);

        String welcomeMessage = String.format("[%s] 님이 채팅방에 참여했습니다.", nickname);
        try {
            chatServer.broadcast(MessageType.MSG_TYPE_SERVER_NOTICE, welcomeMessage.getBytes(StandardCharsets.UTF_8), client.socketFd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleClientDisconnected(ChatServer chatServer, ClientInfo client) {
        String nickname = connectedClients.remove(client.socketFd);

        if (nickname != null) {
            log.info("[{}] 님이 접속을 종료했습니다.", nickname);
            String leaveMessage = String.format("[%s] 님이 채팅방을 나갔습니다.", nickname);
            try {
                chatServer.broadcast(MessageType.MSG_TYPE_SERVER_NOTICE, leaveMessage.getBytes(StandardCharsets.UTF_8), client.socketFd);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void handleMessageReceived(ChatServer chatServer, ClientInfo client, byte[] payload) {
        String nickname = connectedClients.get(client.socketFd);
        String message = new String(payload, StandardCharsets.UTF_8);
        String broadcastMessage = String.format("%s: %s", nickname, message);

        log.info("메시지 수신 <{}>: {}", nickname, message);

        try {
            chatServer.broadcast(MessageType.MSG_TYPE_CHAT_TEXT, broadcastMessage.getBytes(StandardCharsets.UTF_8), client.socketFd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
