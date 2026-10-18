import java.io.*;
import java.net.*;

public class Client {
    private Socket s = null;
    private BufferedReader in = null;
    private DataOutputStream out = null;

    public Client(String addr, int port)
    {

        try {
            // Establish connection
            s = new Socket(addr, port);
            System.out.println("Connected");

            // Read input from terminal
            in = new BufferedReader(
                new InputStreamReader(System.in));

            // Send data to server
            out = new DataOutputStream(s.getOutputStream());

            String m = "";

            // Send messages until "Over" is entered
            while (!m.equals("Over")) {
                m = in.readLine();
                out.writeUTF(m);
            }
        }
        catch (UnknownHostException e) {
            System.out.println(e);
        }
        catch (IOException e) {
            System.out.println(e);
        }
        finally {
            try {
                if (in != null|| out!=null||s!=null) {
                    in.close();
                    out.close();
                    s.close();
                }
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args)
    {
        new Client("127.0.0.1", 5000);
    }
}