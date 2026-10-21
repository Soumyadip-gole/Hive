package agent;

import java.io.*;
import java.net.*;

public class Client {
    private Socket s = null;
    private DataInputStream input = null;
    private DataOutputStream out = null;

    public Client(String addr, int port) {

        try {
            // Establish connection
            s = new Socket(addr, port);
            System.out.println("Connected");

            // Takes input from the client socket
            input = new DataInputStream(new BufferedInputStream(s.getInputStream()));

            // take input from terminal
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

            // Send data to server
            out = new DataOutputStream(s.getOutputStream());

            // Reads message from server until "Over" is sent
            Thread socketReader = new Thread(() -> {
                try {
                    while (true) {
                        String message = input.readUTF();
                        System.out.println("server: " + message);

                        if ("Over".equals(message)) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error caught while reading from socket in client" + e);
                }
            });

            // read from terminal until over is sent
            Thread terminalReader = new Thread(() -> {
                try {
                    while (true) {
                        String message = br.readLine();
                        if (message == null) {
                            break;
                        }

                        out.writeUTF(message);
                        out.flush();

                        if ("Over".equals(message)) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error caught while reading from terminal in server");
                }
            });

            // start the threads
            socketReader.start();
            terminalReader.start();

            // wait for it to finish
            try {
                socketReader.join();
                terminalReader.join();
            } catch (InterruptedException e) {
                System.out.println("error with the threads in server " + e);
            }
        } catch (Exception E) {
            System.out.println("exception in client " + E);
        } finally {
            try {
                out.close();
                s.close();
                input.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        new Client("127.0.0.1", 5000);
    }
}
