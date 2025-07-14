package project.java_chat_server.wrapper_library.callbacks;

import com.sun.jna.Callback;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import project.java_chat_server.wrapper_library.structure.ClientInfo;

public interface ServerOnCompleteMessageReceivedCallback extends Callback {
    void invoke(Pointer userData, ClientInfo.ByReference client, int msgType, Pointer payload, NativeLong len);
}
