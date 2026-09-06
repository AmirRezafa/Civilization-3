package network.client;

import network.protocol.Message;
import network.protocol.MessageCodec;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetworkClient {
    private static final long HEARTBEAT_INTERVAL_MS = 5000;

    private final NetworkListener listener;
    private final Object writeLock = new Object();
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread listenerThread;
    private Thread heartbeatThread;
    private DatagramSocket heartbeatSocket;
    private String host;
    private int port;
    private volatile boolean connected;

    public NetworkClient(NetworkListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        connected = true;
        listenerThread = new Thread(this::listenLoop, "network-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void startHeartbeat(String playerId) {
        try {
            heartbeatSocket = new DatagramSocket();
        } catch (IOException e) {
            return;
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (IOException e) {
            heartbeatSocket.close();
            return;
        }
        byte[] payload = playerId.getBytes(StandardCharsets.UTF_8);
        heartbeatThread = new Thread(() -> heartbeatLoop(address, payload), "udp-heartbeat");
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void heartbeatLoop(InetAddress address, byte[] payload) {
        while (connected) {
            try {
                DatagramPacket packet = new DatagramPacket(payload, payload.length, address, port);
                heartbeatSocket.send(packet);
                Thread.sleep(HEARTBEAT_INTERVAL_MS);
            } catch (IOException | InterruptedException e) {
                break;
            }
        }
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
        if (heartbeatSocket != null) {
            heartbeatSocket.close();
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
