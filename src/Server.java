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
    private static int cantClients = 2;
    private static boolean isFirstFile = false;
    private static MailBox mb;
    private static Object monitor;
    private static String PATH_FILE1 = "./data/prueba1.txt";
    private static String PATH_FILE2 = "./data/prueba2.txt";
    private static LinkedList<ThreadClient> sockets = new LinkedList<ThreadClient>();

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        MailBox mb = new MailBox();
        Server.mb = mb;
        monitor = new Object();
        ThreadClient.setMonitor(monitor);
        ThreadClient.setMailBox(mb);
        ServerSocket sv = new ServerSocket(port);

        while (true) {
            Socket sc = sv.accept();
            sockets.add(new ThreadClient(sc, sockets.size() + 1));

            if (sockets.size() == cantClients)
                serveClients();
        }
    }

    private static void serveClients() throws IOException, NoSuchAlgorithmException {
        int size = sockets.size();
        for (int i = 0; i < size; i++)
            sockets.poll().start();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        File file;
        if (isFirstFile) {
            file = new File(PATH_FILE1);
        } else {
            file = new File(PATH_FILE2);
        }

        byte[] byteFile = new byte[2048];
        BufferedInputStream bf = new BufferedInputStream(new FileInputStream(file));

        int count;
        while ((count = bf.read(byteFile)) != -1) {
            System.out.println("Data Sent : " + count);
            digest.update(byteFile, 0, count);
            mb.setChunk(byteFile);
            synchronized (monitor) {
                monitor.notifyAll();
                System.out.println("Realizo Notify1");
            }
            while (mb.getCount() != cantClients) {
                Thread.yield();
            }
            mb.setCount();
        }

        mb.setFinished(true);
        synchronized (monitor) {
            monitor.notifyAll();
            System.out.println("Realizo Notify2");
        }

        byte[] hashFile = digest.digest();

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

        synchronized public boolean getCan() {
            return can;
        }

        synchronized public void setCan(Boolean res) {
            System.out.println("CAN = " + res);
            can = res;
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
        private final int id;
        private static MailBox mb;
        private final Socket client;

        public ThreadClient(Socket client, int id) {
            this.client = client;
            this.id = id;
        }

        @Override
        public void run() {
            System.out.println("Start Thread");
            OutputStream os = null;
            try {
                os = client.getOutputStream();

                os.write(id);
                os.flush();

                synchronized (monitor) {
                    System.out.println("Entro del wait1");
                    monitor.wait();
                    System.out.println("Salio del wait1");
                }
                while (!mb.getFinished()) {
                    synchronized (monitor) {
                        os.write(mb.getChunk(), 0, mb.getChunk().length);
                        os.flush();
                        mb.addCount();
                        System.out.println("Entro del wait2");
                        monitor.wait();
                        System.out.println("Salio del wait2");
                    }
                }

                System.out.println("End Thread");
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
    }
}