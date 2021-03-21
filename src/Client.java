import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.*;

public class Client {
    private static String filePath;
    private static String serverIP;
    private static String idTest;
    private static int port = 6969;

    private static Logger logger = Logger.getLogger("Client");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static boolean checkHash(byte[] hash1, byte[] hash2) {
        return Arrays.equals(hash1, hash2);
    }

    public static byte[] readFileSocket(InputStream sockInput, int id, long fileSize, String ext) throws Exception {
        byte[] bytes = new byte[1024];
        FileOutputStream out = new FileOutputStream(filePath + "Cliente" + id + "-Prueba-" + idTest + "." + ext);
        BufferedOutputStream buffOut = new BufferedOutputStream(out);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        logger.info("Starting transfer");
        int len;
        long bytesReceived = 0;
        long startT = System.currentTimeMillis();
        while (fileSize > 0 && (len = sockInput.read(bytes, 0, bytes.length)) != -1) {
            if (fileSize - len <= -1) {
                buffOut.write(bytes, 0, (int) fileSize);
                digest.update(bytes, 0, (int) fileSize);
            } else {
                buffOut.write(bytes, 0, len);
                digest.update(bytes, 0, len);
            }
            fileSize -= len;
            bytesReceived += len;
        }
        long finalTime = System.currentTimeMillis() - startT;
        logger.info("File Transfer complete and hash calculated");
        logger.info("Total transfer duration: " + finalTime + " millis");
        logger.info("File Saved in: " + filePath);
        logger.info("Bytes received: " + bytesReceived);
        buffOut.close();
        return digest.digest();
    }

    public static void main(String[] args) throws IOException {
        filePath = args[0];
        serverIP = args[1];
        idTest = args[2];

        Socket socket = null;
        InputStream sockInput = null;
        DataInputStream dis = null;

        OutputStream sockOutput = null;
        DataOutputStream dos = null;

        // Log
        LogManager.getLogManager().reset();
        LocalDateTime currantTime = LocalDateTime.now();
        String parsedTime = DATE_TIME_FORMATTER.format(currantTime);
        FileHandler fileHandler = new FileHandler("./Logs/Client/" + parsedTime + "-log.txt");
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);

        try {
            socket = new Socket(serverIP, port);
            System.out.println("Esperando transmision de archivo");
            sockInput = socket.getInputStream();
            dis = new DataInputStream(sockInput);

            sockOutput = socket.getOutputStream();
            dos = new DataOutputStream(sockOutput);

            dos.writeUTF("ACK Ready");
            dos.flush();

            int id = dis.readInt();
            String ext = dis.readUTF();
            logger.info("Local File name: " + "Cliente" + id + "-Prueba-" + idTest + "." + ext);
            long fileSize = dis.readLong();
            logger.info("Client ID: " + id);

            logger.info("Port: " + socket.getLocalPort());

            logger.info("Size of file that will be transfered: " + fileSize);
            System.out.println("File Size: " + fileSize + " B");
            System.out.println("Empezo la transferencia de archivo");
            byte[] localHash = readFileSocket(sockInput, id, fileSize, ext);
            System.out.println("Transferencia de archivo completa, puede encontrar su archivo en " + filePath);
            logger.info("hash received from server");
            byte[] hash = new byte[32];
            sockInput.read(hash);
            boolean check = checkHash(hash, localHash);
            if (!check) {
                logger.severe("File integrity verification failed");
            } else {
                logger.info("File integrity verification passed");
            }


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
