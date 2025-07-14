package project.java_chat_server.runner;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import project.java_chat_server.service.ChatService;
import project.java_chat_server.wrapper_library.ChatServer;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatServerRunner implements ApplicationRunner {
    private final ChatServer chatServer;
    private final ChatService chatService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("스프링 부트 애플리케이션 시작 완료. 네이티브 채팅 서버 초기화를 시작합니다...");

        chatServer.setOnConnectListener(
                (Pointer userData, ClientInfo client) -> chatService.handleClientConnected(chatServer, client)
        );

        chatServer.setOnDisconnectListener(
                (Pointer userData, ClientInfo client) -> chatService.handleClientDisconnected(chatServer, client)
        );

        chatServer.setOnMessageListener(
                (Pointer userData, ClientInfo client, int msgType, Pointer payload, NativeLong len) -> {
                    byte[] messageBytes = payload.getByteArray(0, len.intValue());
                    chatService.handleMessageReceived(chatServer, client, messageBytes);
                }
        );

        chatServer.setOnErrorListener(
                (Pointer userData, int errorCode, String message) -> log.error("네이티브 에러 발생: (코드 {}) {}", errorCode, message)
        );

        chatServer.start();
        log.info("네이티브 채팅 서버가 포트 {}에서 성공적으로 시작되었습니다.", chatServer.getPort());
    }

}
