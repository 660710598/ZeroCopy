import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class ZeroCopyClient {

    public static void main(String[] args) {

        String HOST = "localhost";
        int PORT = 9999;           
        String file_to_save = "..\\FileSave\\TestFileDownloadedZeroCopy"; 

        try (
            // 2. เปิด SocketChannel และเชื่อมต่อ Server ทันที
            SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
            
            // 3. เปิด FileChannel สำหรับ "เขียน" ลงไฟล์ปลายทาง
            FileChannel fileChannel = new FileOutputStream(file_to_save).getChannel()
        ) {

            System.out.println("Client: Connected to server at " + socketChannel.getRemoteAddress());
            System.out.println("Client: Ready to receive file, saving to " + file_to_save);

            //Zero Copy file transfer Network->Disk
            long startTime = System.currentTimeMillis();
            long totalBytesTransferred = fileChannel.transferFrom(socketChannel, 0, Long.MAX_VALUE);
            long endTime = System.currentTimeMillis();
            System.out.println("Client: File received. Total bytes: " + totalBytesTransferred);
            System.out.println("Client: File transfer time: " + (endTime - startTime) + " ms");

        } catch (IOException e) {
            System.err.println("Client: Error connecting or transferring file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}