package project.java_chat_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import project.java_chat_server.wrapper_library.ChatServer;

@Configuration
public class ChatServerConfig {
    @Value("${chat.server.port}")
    private int port;

    @Value("${chat.server.max-clients}")
    private int maxClients;

    @Bean
    public ChatServer chatServer() {
        return new ChatServer(port, maxClients);
    }

}
