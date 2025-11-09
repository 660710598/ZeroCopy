import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ZeroCopyServer {
    public static void main(String[] args) throws IOException{
        int port = 9999;
        String file_to_send = "..\\FileSend1\\TestFileSent";

        //open ServerSocketChannel and bind port
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){
            serverSocketChannel.bind(new InetSocketAddress(port));
            System.out.println("Server is listening on port " + port);
            while (true) {
                
                System.out.println("\nWaiting for new client connection...");
                try(SocketChannel clientChannel = serverSocketChannel.accept();
                FileChannel fileChannel = new FileInputStream(file_to_send).getChannel()){
                    //เมื่อเชื่อมต่อสำเร็จจะพยายามเปิดไฟล์ต่อทันที

                    System.out.println("client connected: "+ clientChannel.getRemoteAddress());
                    
                    long fileSize = fileChannel.size();
                    System.out.println("Sending file: " + file_to_send + " (" + fileSize + " bytes)");

                    //Zero Copy file transfer Disk->Network
                    long totalBytesTransferred = 0;
                    long startTime = System.currentTimeMillis();

                        while (totalBytesTransferred < fileSize) {
                            //ใช้ transferTo() (Zero Copy)
                            long bytesTransferred = fileChannel.transferTo(
                                totalBytesTransferred, 
                                fileSize - totalBytesTransferred, 
                                clientChannel
                            );
                            totalBytesTransferred += bytesTransferred;
                        }
                    
                    long endTime = System.currentTimeMillis();
                    System.out.println("File sent. Total bytes: " + totalBytesTransferred);
                    System.out.println("File transfer time: " + (endTime - startTime) + " ms");
                
                }catch (IOException e) { //จับ Error ทั้งจาก Client และ File 
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        }
    }
}