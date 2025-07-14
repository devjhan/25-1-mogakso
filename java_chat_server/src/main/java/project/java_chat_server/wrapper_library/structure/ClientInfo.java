package project.java_chat_server.wrapper_library.structure;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

@Structure.FieldOrder({"socketFd", "ipAddr", "clientParser"})
public class ClientInfo extends Structure {
    public int socketFd;
    public byte[] ipAddr = new byte[16];
    public Pointer clientParser;
}

