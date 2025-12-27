import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    // List to hold active client handlers
    private static Set<ClientHandler> clientHandlers = new HashSet<>();
    private static final String LOG_FILE = "chat_logs.txt";

    public static void main(String[] args) {
        System.out.println("Chat Server is running...");

        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                // Accept client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create a new ClientHandler for each client
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandlers.add(clientHandler);

                // Start a new thread for the client
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // Method to broadcast messages to all connected clients
    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            // Send the message to all clients except the sender
            if (client != sender) {
                client.sendMessage(message);
            }
        }
        logMessage(message); // Log the message
    }

    // Send a private message to a specific client
    public static void sendPrivateMessage(String recipientName, String message, ClientHandler sender) {
        for (ClientHandler client : clientHandlers) {
            if (client.getClientName().equalsIgnoreCase(recipientName)) {
                client.sendMessage("[Private] " + sender.getClientName() + ": " + message);
                sender.sendMessage("[Private to " + recipientName + "] " + message);
                return;
            }
        }
        sender.sendMessage("User '" + recipientName + "' not found.");
    }

    // Remove a client from the active list
    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        System.out.println("Client disconnected: " + clientHandler.getClientName());
        broadcastMessage(clientHandler.getClientName() + " has left the chat.", null);
    }

    // Get the list of all connected users
    public static String getUserList() {
        StringBuilder userList = new StringBuilder("Connected users: ");
        for (ClientHandler client : clientHandlers) {
            userList.append(client.getClientName()).append(", ");
        }
        // Remove trailing comma and space
        if (userList.length() > 0) {
            userList.setLength(userList.length() - 2);
        }
        return userList.toString();
    }

    // Log messages to a file
    public static void logMessage(String message) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(message + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to log file: " + e.getMessage());
        }
    }

    // Inner class to handle individual clients
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Set up input and output streams
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Ask for client name
                out.println("Enter your name: ");
                clientName = in.readLine();
                System.out.println(clientName + " has joined the chat.");
                broadcastMessage(clientName + " has joined the chat.", this);

                // Handle messages from the client
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/pm ")) {
                        // Private message
                        String[] parts = message.split(" ", 3);
                        if (parts.length == 3) {
                            sendPrivateMessage(parts[1], parts[2], this);
                        } else {
                            out.println("Invalid private message format. Use: /pm [username] [message]");
                        }
                    } else if (message.equalsIgnoreCase("/users")) {
                        // Send list of connected users
                        out.println(getUserList());
                    } else {
                        // Broadcast message to all clients
                        broadcastMessage(clientName + ": " + message, this);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection error with client: " + e.getMessage());
            } finally {
                // Handle client disconnection
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                removeClient(this);
            }
        }

        // Send a message to this client
        public void sendMessage(String message) {
            out.println(message);
        }

        // Get the name of the client
        public String getClientName() {
            return clientName;
        }
    }
}