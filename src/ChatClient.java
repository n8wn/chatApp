import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 12345);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Scanner scanner = new Scanner(System.in);

            // Start a thread to listen for messages from the server
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    System.err.println("Connection closed: " + e.getMessage());
                }
            }).start();

            // Set the username and send messages to the server
            System.out.println(in.readLine()); // Prompt for name
            String name = scanner.nextLine();
            out.println(name);

            // Keep sending messages to the server
            while (true) {
                String input = scanner.nextLine();

                if (input.equalsIgnoreCase("/exit")) {
                    System.out.println("Exiting the chat...");
                    break;
                }

                out.println(input);
            }
        } catch (IOException e) {
            System.err.println("Error connecting to the server: " + e.getMessage());
        }
    }
}