package network.client;

import network.protocol.Message;
import network.protocol.MessageCodec;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetworkClient {
    private final NetworkListener listener;
    private final Object writeLock = new Object();
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread listenerThread;
    private volatile boolean connected;

    public NetworkClient(NetworkListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        connected = true;
        listenerThread = new Thread(this::listenLoop, "network-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop() {
        String disconnectReason = "Connection closed";
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Message message;
                try {
                    message = MessageCodec.decode(line);
                } catch (RuntimeException e) {
                    continue;
                }
                Message received = message;
                SwingUtilities.invokeLater(() -> listener.onMessageReceived(received));
            }
        } catch (IOException e) {
            disconnectReason = e.getMessage();
        } finally {
            connected = false;
            String reason = disconnectReason;
            SwingUtilities.invokeLater(() -> listener.onDisconnected(reason));
        }
    }

    public void send(Message message) throws IOException {
        if (!connected) {
            throw new IOException("Not connected");
        }
        String json = MessageCodec.encode(message);
        synchronized (writeLock) {
            out.write(json);
            out.newLine();
            out.flush();
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
