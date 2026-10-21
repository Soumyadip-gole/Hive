package coordinator;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket;
    private int clientCounter=1;
    private static List<DataOutputStream> outputs = new CopyOnWriteArrayList<>();

    public Server(int port) {
        // Starts server and waits for a connection
        try {

            serverSocket = new ServerSocket(port);
            System.out.println("Server started");

            System.out.println("Waiting for a client ...");

            // take input from terminal
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            new Thread(() -> sendClient(br)).start();

            while (true) {
                Socket socket = serverSocket.accept();
                int id = clientCounter;
                clientCounter++;
                System.out.println("Client accepted " + id);
                Thread.startVirtualThread(() -> handleClient(socket, id));
            }

        } catch (IOException i) {
            System.out.println(i);
        } finally {
            try {
                serverSocket.close();
            } catch (IOException i) {
                System.out.println(i);
            }
        }
    }

    static void sendClient(BufferedReader br) {
        try {
            String message;
            while (!(message = br.readLine()).equals("Over")) {
                for (DataOutputStream x : outputs) {
                    x.writeUTF(message);
                    x.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Error caught while reading from terminal in server");
        }
    }

    static void handleClient(Socket socket, int id) {
        try {

            // Takes input from the client socket
            DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

            // send data to client
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            outputs.add(output);
            output.writeUTF("accepted as client " + id);
            output.flush();

            // Reads message from client until "Over" is sent
            Thread socketReader = new Thread(() -> {
                try {
                    while (true) {
                        String message = input.readUTF();
                        System.out.println("Client " + id + " : " + message);

                        if ("Over".equals(message)) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error caught while reading from socket in server" + e);
                }
            });

            // // read from terminal until over is sent
            // Thread terminalReader = new Thread(() -> {
            // try {
            // while (true) {
            // String message = br.readLine();
            // if (message == null) {
            // break;
            // }
            //
            // output.writeUTF(message);
            // output.flush();
            //
            // if ("Over".equals(message)) {
            // break;
            // }
            // }
            // } catch (IOException e) {
            // System.out.println("Error caught while reading from terminal in server");
            // }
            // });

            // start the threads
            socketReader.start();
            //terminalReader.start();

            // wait for it to finish
            socketReader.join();
            //terminalReader.join();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String args[]) {
        Server s = new Server(5000);
    }
}
