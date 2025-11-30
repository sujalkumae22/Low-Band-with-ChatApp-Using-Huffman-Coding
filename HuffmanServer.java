import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;

/**
 * Simple multi-client server. For every connection:
 *  - reads an Object (Map<Character,String>) for codeMap
 *  - reads int bitsLength
 *  - reads int byteLength
 *  - reads compressed bytes
 *  - decompresses and prints message
 *  - optionally replies (server reads console input to reply)
 *
 * Protocol: each message is sent as:
 *   writeObject(codeMap);
 *   writeInt(bitsLength);
 *   writeInt(bytes.length);
 *   write(bytes);
 *
 * If client sends null as map -> disconnect.
 */
public class HuffmanServer {

    public static void main(String[] args) throws Exception {
        int port = 5000;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("HuffmanServer listening on port " + port);

        while (true) {
            Socket client = serverSocket.accept();
            System.out.println("Client connected: " + client.getRemoteSocketAddress());
            new Thread(() -> handleClient(client)).start();
        }
    }

    private static void handleClient(Socket sock) {
        try (
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            Scanner sc = new Scanner(System.in)
        ) {
            oos.flush();
            while (true) {
                Object obj = ois.readObject();
                if (obj == null) {
                    System.out.println("Client requested disconnect.");
                    break;
                }
                @SuppressWarnings("unchecked")
                Map<Character, String> map = (Map<Character, String>) obj;
                int bitsLen = ois.readInt();
                int byteLen = ois.readInt();
                byte[] data = new byte[byteLen];
                int read = 0;
                while (read < byteLen) {
                    int r = ois.read(data, read, byteLen - read);
                    if (r < 0) throw new EOFException("Unexpected EOF while reading compressed bytes");
                    read += r;
                }
                String message = Huffman.decompress(map, bitsLen, data);
                System.out.println("Client [" + sock.getRemoteSocketAddress() + "]: " + message);

                // Server reply: for simplicity, echo the message back compressed too
                String reply = "Received: " + message;
                Huffman.CompressedData cd = Huffman.compress(reply);
                oos.writeObject(cd.codeMap);
                oos.writeInt(cd.bitsLength);
                oos.writeInt(cd.bytes.length);
                oos.write(cd.bytes);
                oos.flush();
            }
        } catch (Exception e) {
            System.out.println("Client handler exception: " + e.getMessage());
        } finally {
            try { sock.close(); } catch (IOException ignored) {}
            System.out.println("Connection closed: " + sock.getRemoteSocketAddress());
        }
    }
}
