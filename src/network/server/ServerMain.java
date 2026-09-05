package network.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        GameServer server = new GameServer(port);
        server.start();
        System.out.println("Server listening on port " + port + ". Type 'quit' to stop.");

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = stdin.readLine()) != null) {
            if (line.equalsIgnoreCase("quit")) {
                break;
            }
        }

        server.stop();
        System.out.println("Server stopped.");
    }
}
