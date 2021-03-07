import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.Socket;

public class Client {

    private static String filePath = "";
    private static String serverIP = "";
    private static int port = 6969;

    private static int file_size;
    public static void main(String[] args) {
        int bytesRead;
        String savedPath = "";
        try{
            Socket socket = new Socket(serverIP, port);
            InputStream sockInput = socket.getInputStream();
            byte[] bytes = new byte[2048];
            FileOutputStream out = new FileOutputStream(savedPath);
            BufferedOutputStream buffOut = new BufferedOutputStream(out);

            while(sockInput.read(bytes) > 0){

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
