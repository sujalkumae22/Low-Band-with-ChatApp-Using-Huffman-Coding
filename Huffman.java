import java.io.*;
import java.util.*;

/**
 * Huffman encoder/decoder utilities.
 * Provides:
 *  - buildCodeMap(String)
 *  - compress(String) -> returns CompressedData (map, bitsLength, bytes)
 *  - decompress(map, bitsLength, bytes) -> original string
 */
public class Huffman {

    static class Node implements Comparable<Node>, Serializable {
        char ch;
        int freq;
        Node left, right;
        Node(char ch, int freq) { this.ch = ch; this.freq = freq; }
        Node(Node l, Node r) { this.left = l; this.right = r; this.freq = l.freq + r.freq; }
        public boolean isLeaf() { return left == null && right == null; }
        @Override public int compareTo(Node o) { return this.freq - o.freq; }
    }

    public static class CompressedData implements Serializable {
        public Map<Character, String> codeMap;
        public int bitsLength;
        public byte[] bytes;
        public CompressedData(Map<Character, String> codeMap, int bitsLength, byte[] bytes) {
            this.codeMap = codeMap; this.bitsLength = bitsLength; this.bytes = bytes;
        }
    }

    // Build Huffman tree and code map for the given text
    public static Map<Character, String> buildCodeMap(String text) {
        if (text == null || text.length() == 0) return new HashMap<>();
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : text.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (Map.Entry<Character,Integer> e : freq.entrySet()) pq.add(new Node(e.getKey(), e.getValue()));
        // If only one unique char, create a dummy node so codes have length >=1
        if (pq.size() == 1) {
            pq.add(new Node('\0', 0));
        }
        while (pq.size() > 1) {
            Node a = pq.poll(), b = pq.poll();
            pq.add(new Node(a, b));
        }
        Node root = pq.poll();
        Map<Character, String> codeMap = new HashMap<>();
        buildCodeRec(root, "", codeMap);
        return codeMap;
    }

    private static void buildCodeRec(Node node, String prefix, Map<Character,String> map) {
        if (node == null) return;
        if (node.isLeaf()) {
            map.put(node.ch, prefix.length() > 0 ? prefix : "0"); // single char guard
            return;
        }
        buildCodeRec(node.left, prefix + '0', map);
        buildCodeRec(node.right, prefix + '1', map);
    }

    // compress text -> CompressedData
    public static CompressedData compress(String text) {
        Map<Character, String> codeMap = buildCodeMap(text);
        StringBuilder bits = new StringBuilder();
        for (char c : text.toCharArray()) {
            bits.append(codeMap.get(c));
        }
        int bitsLen = bits.length();
        byte[] bytes = packBits(bits.toString());
        return new CompressedData(codeMap, bitsLen, bytes);
    }

    // unpack bytes using map -> original string
    public static String decompress(Map<Character, String> codeMap, int bitsLength, byte[] bytes) {
        if (codeMap == null || codeMap.isEmpty()) return "";
        // build reverse map
        Map<String, Character> rev = new HashMap<>();
        for (Map.Entry<Character,String> e : codeMap.entrySet()) rev.put(e.getValue(), e.getKey());

        String bitString = unpackBits(bytes, bitsLength);
        StringBuilder out = new StringBuilder();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < bitString.length(); i++) {
            cur.append(bitString.charAt(i));
            if (rev.containsKey(cur.toString())) {
                out.append(rev.get(cur.toString()));
                cur.setLength(0);
            }
        }
        return out.toString();
    }

    // Helpers: pack bits (string of '0'/'1') into bytes (MSB-first within each byte)
    private static byte[] packBits(String bits) {
        int len = bits.length();
        int byteLen = (len + 7) / 8;
        byte[] out = new byte[byteLen];
        for (int i = 0; i < len; i++) {
            int byteIndex = i / 8;
            int bitIndex = 7 - (i % 8); // MSB first
            if (bits.charAt(i) == '1') out[byteIndex] |= (1 << bitIndex);
        }
        return out;
    }

    // unpack bytes into bitstring up to bitsLength
    private static String unpackBits(byte[] bytes, int bitsLength) {
        StringBuilder sb = new StringBuilder(bitsLength);
        for (int i = 0; i < bitsLength; i++) {
            int byteIndex = i / 8;
            int bitIndex = 7 - (i % 8);
            int bit = (bytes[byteIndex] >> bitIndex) & 1;
            sb.append(bit == 1 ? '1' : '0');
        }
        return sb.toString();
    }
}
