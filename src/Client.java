import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class Client {
    private static String filePath;
    private static String serverIP;
    private static int port = 6969;

    public static boolean checkHash(byte[] hash1, byte[] hash2) {
        return Arrays.equals(hash1, hash2);
    }

    public static byte[] readFileSocket(InputStream sockInput, int id, long fileSize) throws Exception {
        byte[] bytes = new byte[4096];
        FileOutputStream out = new FileOutputStream(filePath + id + ".txt");
        BufferedOutputStream buffOut = new BufferedOutputStream(out);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        int len;
        while (fileSize > 0 && (len = sockInput.read(bytes, 0, bytes.length)) != -1) {
            buffOut.write(bytes, 0, len);
            digest.update(bytes, 0, len);
            fileSize -= len;
        }
        buffOut.close();
        return digest.digest();
    }

    public static void main(String[] args) throws IOException {
        filePath = args[0];
        serverIP = args[1];

        Socket socket = null;
        InputStream sockInput = null;
        DataInputStream dis = null;

        OutputStream sockOutput = null;
        DataOutputStream dos = null;

        try {
            socket = new Socket(serverIP, port);
            sockInput = socket.getInputStream();
            dis = new DataInputStream(sockInput);

            sockOutput = socket.getOutputStream();
            dos = new DataOutputStream(sockOutput);

            dos.writeUTF("ACK Ready");
            dos.flush();

            int id = dis.readInt();
            long fileSize = dis.readLong();
            System.out.println(fileSize);

            byte[] localHash = readFileSocket(sockInput, id, fileSize);

            byte[] hash = new byte[32];
            sockInput.read(hash);
            boolean check = checkHash(hash, localHash);
            if (!check) {
                System.out.println("Archivo Corrupto!!!!! UwU");
            } else
                System.out.println("Se logrooooo!!!!! UwU");


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (sockInput != null) sockInput.close();
            if (dis != null) dis.close();
            if (socket != null) socket.close();
            if (sockOutput != null) sockOutput.close();
            if (dos != null) dos.close();
        }
    }
}
