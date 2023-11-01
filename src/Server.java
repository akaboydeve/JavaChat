import java.io.*;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private ArrayList<connectionHandler> connections;
    private ServerSocket server;
    private boolean done;

    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(15001);
            System.out.println("Server Started on port 15001");
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                connectionHandler handler = new connectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }

        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (connectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (connectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            // ignore
        }

    }

    class connectionHandler implements Runnable {
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public connectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please Enter a Nickname: ");
                nickname = in.readLine();
                System.out.println(nickname + " Connected");
                broadcast(nickname + " Joined the Chat!");
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println(nickname + " typed " + message);
                    if (message.startsWith("/nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " Renamed to " + messageSplit[1]);
                            System.out.println(nickname + " Renamed to " + messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully changed nickname to " + nickname);
                        } else {
                            out.println("No nickname provided!");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname + " Lest the chat!");
                        shutdown();
                    } else {
                        broadcast(nickname + " : " + message);

                    }
                }

            } catch (IOException e) {
                shutdown();
            }

        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

}
