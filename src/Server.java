import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.*;

public class Server {
    private static int port = 6969;
    private static Logger logger = Logger.getLogger("Server");
    private static int cantClients;
    private static MailBox mb;
    private static Object monitor;
    private static String PATH_FILE;
    private static LinkedList<ThreadClient> sockets = new LinkedList<ThreadClient>();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException {
        PATH_FILE = args[0];
        cantClients = Integer.parseInt(args[1]);
        MailBox mb = new MailBox();
        Server.mb = mb;
        monitor = new Object();
        ThreadClient.setMonitor(monitor);
        ThreadClient.setMailBox(mb);
        ServerSocket sv = new ServerSocket(port);

        // Log
        LogManager.getLogManager().reset();
        LocalDateTime currantTime = LocalDateTime.now();
        String parsedTime = DATE_TIME_FORMATTER.format(currantTime);
        FileHandler fileHandler = new FileHandler("./Logs/Server/" + parsedTime + "-log.txt");
        fileHandler.setFormatter(new SimpleFormatter());
        fileHandler.setLevel(Level.ALL);
        logger.addHandler(fileHandler);

        while (true) {
            Socket sc = sv.accept();
            DataInputStream in = new DataInputStream(sc.getInputStream());
            String confirmation = in.readUTF();
            if (confirmation.equals("ACK Ready")) {
                System.out.println("Acepto un cliente");
                sockets.add(new ThreadClient(sc, sockets.size() + 1));

                if (sockets.size() == cantClients) {
                    serveClients();
                    break;
                }
            }
        }

        sv.close();
    }

    private static void serveClients() throws IOException, NoSuchAlgorithmException, InterruptedException {
        File file = new File(PATH_FILE);
        // Log
        logger.info("File name: " + file.getName());
        int fileSize = (int) file.length();
        // Log
        logger.info("File size: " + fileSize + " B");
        ThreadClient.setSizeFile(fileSize);

        int size = sockets.size();
        for (int i = 0; i < size; i++)
            sockets.get(i).start();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] byteFile = new byte[1024];
        BufferedInputStream bf = new BufferedInputStream(new FileInputStream(file));

        int count;
        while ((count = bf.read(byteFile)) != -1) {
            digest.update(byteFile, 0, count);
            mb.setChunk(byteFile);
            synchronized (monitor) {
                monitor.notifyAll();
            }
            while (mb.getCount() != cantClients) {
                Thread.yield();
            }
            mb.setCount();
        }

        mb.setFinished(true);
        byte[] hashFile = digest.digest();
        ThreadClient.setHashFile(hashFile);
        synchronized (monitor) {
            monitor.notifyAll();
        }

        for (int i = 0; i < sockets.size(); i++){
            sockets.get(i).join();
        }

        bf.close();
        for(Handler h: logger.getHandlers()){
            h.close();
        }
    }

    private static class MailBox {
        private byte[] chunk;
        private int count = 0;
        private boolean finished = false;

        synchronized public boolean getFinished() {
            return finished;
        }

        synchronized public void setFinished(Boolean res) {
            finished = res;
        }

        synchronized public void setChunk(byte[] chunk) {
            this.chunk = chunk;
        }

        synchronized public byte[] getChunk() {
            return chunk;
        }

        synchronized public void addCount() {
            count++;
        }

        synchronized public int getCount() {
            return count;
        }

        synchronized public void setCount() {
            count = 0;
        }
    }

    private static class ThreadClient extends Thread {

        private static byte[] hashFile;
        private static Object monitor;
        private static MailBox mb;
        private static long sizeFile;
        private final int id;
        private final Socket client;

        public ThreadClient(Socket client, int id) {
            this.client = client;
            this.id = id;
        }

        @Override
        public void run() {
            // Log
            logger.info("Start Thread " + id);
            OutputStream os = null;
            DataOutputStream dataOs = null;
            try {
                os = client.getOutputStream();
                dataOs = new DataOutputStream(os);

                dataOs.writeInt(id);
                dataOs.flush();

                dataOs.writeLong(sizeFile);
                dataOs.flush();

                synchronized (monitor) {
                    monitor.wait();
                }

                long start = System.currentTimeMillis();
                int i = 0;
                int bytes = 0;
                while (!mb.getFinished()) {
                    synchronized (monitor) {
                        os.write(mb.getChunk(), 0, mb.getChunk().length);
                        os.flush();
                        i++;
                        bytes += mb.getChunk().length;
                        mb.addCount();
                        monitor.wait();
                    }
                }

                // Log
                logger.info("Thread " + id + ", Total number of packages sent: " + i + ", With total size: " + bytes + " B");
                long elapsedTime = System.currentTimeMillis() - start;
                logger.info("Thread " + id + " Elapsed Time " + elapsedTime + " millis");

                os.write(hashFile);
                os.flush();

                // Log
                logger.info("Successful File Transfer Thread " + id);
            } catch (IOException | InterruptedException e) {
                logger.severe("Thread " + id + " couldn't finalize properly");
            } finally {
                try {
                    if (os != null) os.close();
                    if (dataOs != null) dataOs.close();
                    client.close();
                } catch (IOException e) {

                }
            }
        }

        public static void setHashFile(byte[] hashFile) {
            ThreadClient.hashFile = hashFile;
        }

        public static void setMailBox(MailBox mb) {
            ThreadClient.mb = mb;
        }

        public static void setMonitor(Object monitor) {
            ThreadClient.monitor = monitor;
        }

        public static void setSizeFile(int sizeFile) {
            ThreadClient.sizeFile = sizeFile;
        }
    }
}