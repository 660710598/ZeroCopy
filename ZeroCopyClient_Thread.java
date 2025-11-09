import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.RandomAccessFile; 
import java.util.ArrayList;
import java.util.List;

public class ZeroCopyClient_Thread {

    static  String HOST = "localhost";
    static  int PORT = 9999;
    static  String FILE_TO_SAVE = "..\\\\FileSave\\\\TestFileDownloadedZeroCopy";
    static  int NUM_THREADS = 4; 

    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println("Chunked Client Started...");
        System.out.println("Will connect to " + HOST + ":" + PORT + " with " + NUM_THREADS + " threads.");
        long startTime = System.currentTimeMillis();

        
        long fileSize = getFileSize();
        if (fileSize == -1) {
            System.err.println("Could not get file size.");
            return;
        }
        System.out.println("Total file size: " + fileSize + " bytes (" + (fileSize / 1024 / 1024) + " MB)");

        
        try (RandomAccessFile raf = new RandomAccessFile(FILE_TO_SAVE, "rw")) {
            raf.setLength(fileSize); // จองพื้นที่ไฟล์ให้เท่าขนาดจริง
        }
        System.out.println("Created placeholder file: " + FILE_TO_SAVE);

       
        long partSize = fileSize / NUM_THREADS;
        long currentStart = 0;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            long currentEnd;
            if (i == NUM_THREADS - 1) {
                currentEnd = fileSize - 1; // Thread สุดท้าย เอาที่เหลือทั้งหมด
            } else {
                currentEnd = currentStart + partSize - 1;
            }

            // สร้าง "คนงาน"
            Runnable worker = new FileDownloaderHandler(currentStart, currentEnd);
            Thread t = new Thread(worker);
            t.start();
            threads.add(t);

            System.out.println("  Started Thread " + i + ": (bytes " + currentStart + " to " + currentEnd + ")");
            currentStart += partSize;
        }

        for (Thread t : threads) {
            t.join(); // รอจนกว่า Thread ทุกคนจะทำงานเสร็จ
        }

        long endTime = System.currentTimeMillis();
        System.out.println("\nDownload complete!");
        System.out.println("Total time: " + (endTime - startTime) + " ms");
    }

    private static long getFileSize() throws IOException {
        try (SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(HOST, PORT));
             DataInputStream in = new DataInputStream(socketChannel.socket().getInputStream());
             DataOutputStream out = new DataOutputStream(socketChannel.socket().getOutputStream())) {

            out.writeUTF("SIZE"); // ส่งคำสั่ง
            out.flush();
            return in.readLong(); // รับขนาด (long)

        } catch (IOException e) {
            System.err.println("Error getting file size: " + e.getMessage());
            return -1;
        }
    }

    public static class FileDownloaderHandler implements Runnable {
        private long start;
        private long end;

        public FileDownloaderHandler(long start, long end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public void run() {
            try (
                
                SocketChannel serverSocket = SocketChannel.open(new InetSocketAddress(HOST, PORT));
                RandomAccessFile raf = new RandomAccessFile(FILE_TO_SAVE, "rw");
                FileChannel fc = raf.getChannel(); // เอา Channel ออกมา
                DataOutputStream out = new DataOutputStream(serverSocket.socket().getOutputStream())
            ) { 
                out.writeUTF("GET");
                out.writeLong(start);
                out.writeLong(end);
                out.flush();
            
                long total = 0;
                long size = end - start + 1;
                while (total < size) {
                    long transferred = fc.transferFrom(serverSocket, start + total, size - total);
                    if (transferred <= 0 && total < size) {
                         throw new IOException("Server stopped sending data unexpectedly.");
                    }
                    total += transferred;
                }
                
            } catch (Exception e) {
                System.err.println("  Error in Thread (bytes " + start + "): " + e.getMessage());
            }
        }
    }
}
