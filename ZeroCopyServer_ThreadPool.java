import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ExecutorService; 
import java.util.concurrent.Executors;       

public class ZeroCopyServer_ThreadPool { 

    
    
    //สร้าง พนักงาน 10 คน รอไว้
    static  int NUM_WORKER_THREADS = 10;
    static  ExecutorService executor = Executors.newFixedThreadPool(NUM_WORKER_THREADS);

    public static void main(String[] args) throws IOException {
        int PORT = 9999;
        String FILE_TO_SEND = "..\\\\FileSend1\\\\TestFileSent";
        
        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(PORT));
        System.out.println("Server (ThreadPool) is listening on port " + PORT);
        System.out.println("Running with " + NUM_WORKER_THREADS + " worker threads.");

        while (true) {
            SocketChannel clientSocket = serverSocket.accept();
            System.out.println("\nServer : Accepted Connection from " + clientSocket.getRemoteAddress());

            DataInputStream in = new DataInputStream(clientSocket.socket().getInputStream());
            String cmd = in.readUTF();

            if (cmd.equals("SIZE")) {
                // งาน "SIZE" มันเล็กมาก ให้ Main Thread ทำเองได้เลย
                System.out.println("Server : Client requested file size.");
                try (FileChannel fileChannel = new FileInputStream(FILE_TO_SEND).getChannel()) {
                    long fileSize = fileChannel.size();
                    DataOutputStream out = new DataOutputStream(clientSocket.socket().getOutputStream());
                    out.writeLong(fileSize);
                    out.flush();
                }
                clientSocket.close(); 

            } else {
                System.out.println("Server : Client requested chunk. Submitting to thread pool...");
                Runnable handler = new FileSenderHandler(clientSocket, FILE_TO_SEND);
                executor.submit(handler);
                
                
            }
        }
    }

    public static class FileSenderHandler implements Runnable {
        private SocketChannel clientSocket;
        private String fileToSend;

        public FileSenderHandler(SocketChannel clientSocket, String fileToSend) {
            this.clientSocket = clientSocket;
            this.fileToSend = fileToSend;
        }

        @Override
        public void run() {
            try (SocketChannel channel = this.clientSocket; 
                 FileChannel fc = new FileInputStream(fileToSend).getChannel();
                 DataInputStream in = new DataInputStream(channel.socket().getInputStream())) {

                long start = in.readLong();
                long end = in.readLong();
                long size = end - start + 1;
                System.out.println("  (Thread " + Thread.currentThread().getId() + ") Sending bytes " + start + " to " + end);

                long total = 0;
                while (total < size) {
                    long transferred = fc.transferTo(start + total, size - total, channel);
                    total += transferred;
                }
                System.out.println("  (Thread " + Thread.currentThread().getId() + ") Sent " + total + " bytes.");

            } catch (Exception e) {
                System.err.println("  (Thread " + Thread.currentThread().getId() + ") Error: " + e.getMessage());
            }
        }
    }
}