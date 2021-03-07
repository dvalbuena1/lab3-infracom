import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private static int port = 6969;
    private static int cantidadClientesMin = 5;
    private static String PATH_FILE1 = "./data/prueba.txt";
    private static String PATH_FILE2 = "./data/prueba.txt";
    private static LinkedList<ThreadCliente> sockets = new LinkedList<ThreadCliente>();

    public static void main(String[] args) throws IOException {
        ServerSocket sv = new ServerSocket(port);

        File file1 = new File(PATH_FILE1);
        byte [] byteFile1  = new byte [(int)file1.length()];
        BufferedInputStream bf1 = new BufferedInputStream(new FileInputStream(file1));
        bf1.read(byteFile1,0,byteFile1.length);
        bf1.close();

        File file2 = new File(PATH_FILE2);
        byte [] byteFile2  = new byte [(int)file2.length()];
        BufferedInputStream bf2 = new BufferedInputStream(new FileInputStream(file2));
        bf1.read(byteFile2,0,byteFile2.length);
        bf2.close();

        while (true) {
            Socket sc = sv.accept();
            sockets.add(new ThreadCliente(sc, byteFile1));

            if (sockets.size() == cantidadClientesMin)
                for (int i = 0; i < sockets.size(); i++)
                    sockets.poll().start();

        }
    }

    private static class ThreadCliente extends Thread {

        private static byte[] fileBytes;
        private Socket cliente;

        public ThreadCliente(Socket cliente, byte[] fileBytes) {
            this.cliente = cliente;
            this.fileBytes = fileBytes;
        }

        @Override
        public void run() {
            OutputStream os = null;
            try {
                os = cliente.getOutputStream();
                os.write(fileBytes, 0, fileBytes.length);
                os.flush();
            } catch (IOException e) {
            } finally {
                try {
                    if (os != null) os.close();
                    cliente.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
