package network.client;

import network.protocol.Message;

public interface NetworkListener {
    void onMessageReceived(Message message);

    void onDisconnected(String reason);
}
