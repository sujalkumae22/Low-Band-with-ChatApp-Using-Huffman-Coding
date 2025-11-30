import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.Scanner;

/**
 * Simple client:
 *  - connects to server
 *  - reads input lines from console
 *  - compresses and sends (map, bitsLength, bytes)
 *  - reads reply from server in same format and prints
 *
 * To exit: type "/quit"
 */
public class HuffmanClient {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 5000;
        try (Socket socket = new Socket(host, port)) {
            System.out.println("Connected to server " + host + ":" + port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Scanner sc = new Scanner(System.in);

            while (true) {
                System.out.print("You: ");
                String line = sc.nextLine();
                if (line.equalsIgnoreCase("/quit")) {
                    // send null map as signal (server treats null as disconnect)
                    oos.writeObject(null);
                    oos.flush();
                    break;
                }

                Huffman.CompressedData cd = Huffman.compress(line);
                oos.writeObject(cd.codeMap);
                oos.writeInt(cd.bitsLength);
                oos.writeInt(cd.bytes.length);
                oos.write(cd.bytes);
                oos.flush();

                // read reply
                Object obj = ois.readObject();
                @SuppressWarnings("unchecked")
                Map<Character, String> map = (Map<Character, String>) obj;
                int bitsLen = ois.readInt();
                int byteLen = ois.readInt();
                byte[] data = new byte[byteLen];
                int read = 0;
                while (read < byteLen) {
                    int r = ois.read(data, read, byteLen - read);
                    if (r < 0) throw new EOFException("Unexpected EOF while reading reply bytes");
                    read += r;
                }
                String reply = Huffman.decompress(map, bitsLen, data);
                System.out.println("Server: " + reply);
            }
        }
        System.out.println("Disconnected.");
    }
}
