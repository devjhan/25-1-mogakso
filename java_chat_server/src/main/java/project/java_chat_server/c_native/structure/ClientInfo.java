package project.java_chat_server.c_native.structure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

public class ClientInfo extends Structure {
    public static class ByReference extends ClientInfo implements Structure.ByReference {}

    public int socketFd;
    public byte[] ipAddr = new byte[16];
    public Pointer clientParser;

    @Override
    public List<String> getFieldOrder() {
        return Arrays.asList("socketFd", "ipAddr", "clientParser");
    }
}
