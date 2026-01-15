package project.java_chat_server.test_utils;

import project.java_chat_server.wrapper_library.structure.ClientInfo;

import java.nio.charset.StandardCharsets;

public class TestUtils {
    public static ClientInfo createClientInfo(int socketFd, String ipAddr) {
        ClientInfo client = new ClientInfo();
        client.socketFd = socketFd;
        byte[] ipBytes = ipAddr.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(ipBytes, 0, client.ipAddr, 0, Math.min(ipBytes.length, client.ipAddr.length));
        client.write();
        return client;
    }
}
