package project.java_chat_server.runner;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import project.java_chat_server.service.ChatService;
import project.java_chat_server.wrapper_library.ChatServer;
import project.java_chat_server.wrapper_library.enums.MessageType;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

    @PreDestroy
    public void onShutdown() {
        log.info("애플리케이션 종료 신호 감지. Graceful shutdown을 시작합니다...");
        try {
            String shutdownMessage = "알림: 서버가 5초 후 종료됩니다.";
            chatServer.broadcast(
                    MessageType.MSG_TYPE_SERVER_NOTICE,
                    shutdownMessage.getBytes(StandardCharsets.UTF_8),
                    -1
            );
            // 클라이언트가 메시지를 받을 시간을 잠시 기다려줍니다.
            Thread.sleep(5000);
        } catch (IOException e) {
            log.error("종료 공지 방송 중 에러 발생", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
            log.info("Graceful shutdown 완료.");
        }
    }
}
