import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class BenchmarkCopy {

   
   
    public static void main(String[] args) throws IOException {
        String SOURCE_FILE = "C:\\Users\\realr\\OneDrive\\เอกสาร\\ZeroCopy1\\FIleSend1\\TestFileSent"; 
        String SAVE_FOLDER_PATH = "C:\\Users\\realr\\OneDrive\\เอกสาร\\ZeroCopy1\\FileSave\\";
        String DEST_NORMAL = "dest_normal_copy.dat";
        String DEST_ZERO_COPY = "dest_zero_copy.dat";

        File source = new File(SOURCE_FILE);
        long fileSize = source.length();

        System.out.println("Starting benchmark with file: " + SOURCE_FILE);
        System.out.println("File size: " + (fileSize / (1024*1024)) + " MB");
        System.out.println("----------------------------------------");

        //จับเวลาแบบ Normal Copy
        long startTimeNormal = System.currentTimeMillis();
        normalCopy(SOURCE_FILE, DEST_NORMAL);
        long endTimeNormal = System.currentTimeMillis();
        long timeNormal = endTimeNormal - startTimeNormal;
        
        System.out.println("Normal Copy Time:   " + timeNormal + " ms");

        //จับเวลาแบบ Zero Copy
        long startTimeZero = System.currentTimeMillis();
        zeroCopy(SOURCE_FILE, DEST_ZERO_COPY);
        long endTimeZero = System.currentTimeMillis();
        long timeZero = endTimeZero - startTimeZero;

        System.out.println("Zero Copy Time:     " + timeZero + " ms");
        System.out.println("----------------------------------------");
        
        // ป้องกันการหารด้วย 0 ถ้าเร็วมาก
        if (timeZero > 0) {
            long difference = timeNormal - timeZero;
            System.out.println("Difference (Normal - Zero): " + difference + " ms");
        } else {
            System.out.println("Both operations were extremely fast (or time was 0).");
        }

        // 4. ลบไฟล์ที่คัดลอกมาทิ้ง
        new File(DEST_NORMAL).delete();
        new File(DEST_ZERO_COPY).delete();
        System.out.println("Benchmark finished. Destination files deleted.");
        
    }

    public static void normalCopy(String from, String to) throws IOException {
        try (FileInputStream fis = new FileInputStream(from);
             FileOutputStream fos = new FileOutputStream(to)) {
            
            byte[] buffer = new byte[8192]; // 8 KB buffer
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void zeroCopy(String from, String to) throws IOException {
        try (FileChannel source = new FileInputStream(from).getChannel();
             FileChannel destination = new FileOutputStream(to).getChannel()) {
        
            // สั่ง OS ย้ายข้อมูลโดยตรง
            source.transferTo(0, source.size(), destination);
        }
    }
}