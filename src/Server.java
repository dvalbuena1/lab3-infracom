import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

public class Server {
    private static int port = 6969;
    private static Logger logger = Logger.getLogger("MyLogger");
    private static int cantClients;
    private static MailBox mb;
    private static Object monitor;
    private static String PATH_FILE;
    private static LinkedList<ThreadClient> sockets = new LinkedList<ThreadClient>();

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        PATH_FILE = args[0];
        cantClients = Integer.parseInt(args[1]);
        MailBox mb = new MailBox();
        Server.mb = mb;
        monitor = new Object();
        ThreadClient.setMonitor(monitor);
        ThreadClient.setMailBox(mb);
        ServerSocket sv = new ServerSocket(port);

        while (true) {
            Socket sc = sv.accept();
            System.out.println("Acepto un cliente");
            sockets.add(new ThreadClient(sc, sockets.size() + 1));

            if (sockets.size() == cantClients)
                serveClients();
        }
    }

    private static void serveClients() throws IOException, NoSuchAlgorithmException {
        File file = new File(PATH_FILE);
        int fileSize = (int) file.length();
        ThreadClient.setSizeFile(fileSize);

        int size = sockets.size();
        for (int i = 0; i < size; i++)
            sockets.poll().start();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        byte[] byteFile = new byte[4096];
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

        bf.close();
    }

    private static class MailBox {
        private byte[] chunk;
        private int count = 0;
        private boolean can = false;
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
            System.out.println("Start Thread " + id);
            OutputStream os = null;
            try {
                os = client.getOutputStream();

                os.write(id);
                os.flush();
                DataOutputStream dos = new DataOutputStream(os);
                dos.writeLong(sizeFile);
                dos.flush();

                synchronized (monitor) {
                    monitor.wait();
                }
                while (!mb.getFinished()) {
                    synchronized (monitor) {
                        os.write(mb.getChunk(), 0, mb.getChunk().length);
                        os.flush();
                        mb.addCount();
                        monitor.wait();
                    }
                }

                os.write(hashFile);

                System.out.println("End Thread " + id);
            } catch (IOException | InterruptedException e) {
            } finally {
                try {
                    if (os != null) os.close();
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