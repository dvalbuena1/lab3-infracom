import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Arrays;

public class Client {

    private static String filePath = "";
    private static String serverIP = "";
    private static int port = 6969;

    public static boolean checkHash(byte[] hash1, byte[] hash2) {
        return Arrays.equals(hash1, hash2);
    }

    public static void readFileSocket(InputStream sockInput, int id) throws Exception {
        byte[] bytes = new byte[2048];
        FileOutputStream out = new FileOutputStream(filePath + id + ".txt");
        BufferedOutputStream buffOut = new BufferedOutputStream(out);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        int len;
        while ((len = sockInput.read(bytes)) != -1) {
            buffOut.write(bytes, 0, len);
            digest.update(bytes, 0, len);
        }
        buffOut.close();
    }

    public static void notifyServer(PrintWriter pw) {

    }

    public static void main(String[] args) {
        int bytesRead;
        String savedPath = "Cliente";
        try {
            Socket socket = new Socket(serverIP, port);
            InputStream sockInput = socket.getInputStream();
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            int id = sockInput.read();

            readFileSocket(sockInput, id);

            //int id = 0;
            //
            //readFileSocket(sockInput, id);

//            byte[] hash = new byte[32];
//            int bytesHash = sockInput.read(hash);
//
//            if (bytesHash != 32)
//                throw new Exception("Bytes were not read completely");

            sockInput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
