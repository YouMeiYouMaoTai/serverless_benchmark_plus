package test.functions;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
import com.google.gson.JsonObject;// ！！！请勿修改此文件，此文件由脚本生成
import java.nio.ByteBuffer;// ！！！请勿修改此文件，此文件由脚本生成
import java.io.InputStream;// ！！！请勿修改此文件，此文件由脚本生成
import io.minio.MinioClient;// ！！！请勿修改此文件，此文件由脚本生成
import io.minio.GetObjectArgs;// ！！！请勿修改此文件，此文件由脚本生成
import io.minio.GetObjectResponse;// ！！！请勿修改此文件，此文件由脚本生成
import io.minio.PutObjectArgs;// ！！！请勿修改此文件，此文件由脚本生成
import java.awt.Graphics2D;// ！！！请勿修改此文件，此文件由脚本生成
import java.awt.Image;// ！！！请勿修改此文件，此文件由脚本生成
import java.awt.image.BufferedImage;// ！！！请勿修改此文件，此文件由脚本生成
import java.io.ByteArrayInputStream;// ！！！请勿修改此文件，此文件由脚本生成
import java.io.ByteArrayOutputStream;// ！！！请勿修改此文件，此文件由脚本生成
import java.io.IOException;// ！！！请勿修改此文件，此文件由脚本生成
import java.io.InputStream;// ！！！请勿修改此文件，此文件由脚本生成
import java.nio.ByteBuffer;// ！！！请勿修改此文件，此文件由脚本生成
import javax.imageio.ImageIO;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
public class Resize {// ！！！请勿修改此文件，此文件由脚本生成
    // Initialize Minio Client// ！！！请勿修改此文件，此文件由脚本生成
    MinioClient minioClient =// ！！！请勿修改此文件，此文件由脚本生成
            MinioClient.builder()// ！！！请勿修改此文件，此文件由脚本生成
                    .endpoint("http://192.168.31.96:9009")// ！！！请勿修改此文件，此文件由脚本生成
                    .credentials("minioadmin", "minioadmin123")// ！！！请勿修改此文件，此文件由脚本生成
                    .build();// ！！！请勿修改此文件，此文件由脚本生成
    // 辅助方法：将输入流读取到 ByteBuffer// ！！！请勿修改此文件，此文件由脚本生成
    private static ByteBuffer readToByteBuffer(InputStream inputStream) throws IOException {// ！！！请勿修改此文件，此文件由脚本生成
        // Start with a default buffer size// ！！！请勿修改此文件，此文件由脚本生成
        ByteBuffer byteBuffer = ByteBuffer.allocate(10000);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        // Read data from InputStream and write to ByteBuffer// ！！！请勿修改此文件，此文件由脚本生成
        byte[] buffer = new byte[1000];// ！！！请勿修改此文件，此文件由脚本生成
        int bytesRead;// ！！！请勿修改此文件，此文件由脚本生成
        while ((bytesRead = inputStream.read(buffer)) != -1) {// ！！！请勿修改此文件，此文件由脚本生成
            // Ensure there's enough space in the ByteBuffer// ！！！请勿修改此文件，此文件由脚本生成
            if (byteBuffer.remaining() < bytesRead) {// ！！！请勿修改此文件，此文件由脚本生成
                // Expand ByteBuffer// ！！！请勿修改此文件，此文件由脚本生成
                byteBuffer = expandBuffer(byteBuffer, bytesRead);// ！！！请勿修改此文件，此文件由脚本生成
            }// ！！！请勿修改此文件，此文件由脚本生成
            byteBuffer.put(buffer, 0, bytesRead);// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        // Reset ByteBuffer's position, ready for reading// ！！！请勿修改此文件，此文件由脚本生成
        byteBuffer.flip();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        return byteBuffer;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
    // Method to expand the ByteBuffer// ！！！请勿修改此文件，此文件由脚本生成
    private static ByteBuffer expandBuffer(ByteBuffer byteBuffer, int additionalCapacity) {// ！！！请勿修改此文件，此文件由脚本生成
        // Calculate new capacity// ！！！请勿修改此文件，此文件由脚本生成
        int newCapacity = byteBuffer.capacity() + Math.max(additionalCapacity, byteBuffer.capacity());// ！！！请勿修改此文件，此文件由脚本生成
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);// ！！！请勿修改此文件，此文件由脚本生成
        byteBuffer.flip(); // Prepare buffer for copying// ！！！请勿修改此文件，此文件由脚本生成
        newBuffer.put(byteBuffer);// ！！！请勿修改此文件，此文件由脚本生成
        return newBuffer;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
    public static String renameFile(String originalFilename) {// ！！！请勿修改此文件，此文件由脚本生成
        // Find the last dot in the filename// ！！！请勿修改此文件，此文件由脚本生成
        int dotIndex = originalFilename.lastIndexOf('.');// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        // If there is no dot, or it's the first character, handle it// ！！！请勿修改此文件，此文件由脚本生成
        if (dotIndex == -1 || dotIndex == 0) {// ！！！请勿修改此文件，此文件由脚本生成
            throw new IllegalArgumentException("Invalid file name format: " + originalFilename);// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        // Extract the base name (without extension)// ！！！请勿修改此文件，此文件由脚本生成
        String baseName = originalFilename.substring(0, dotIndex);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        // Extract the extension// ！！！请勿修改此文件，此文件由脚本生成
        String extension = originalFilename.substring(dotIndex);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        // Construct the new filename// ！！！请勿修改此文件，此文件由脚本生成
        String newFilename = baseName + "_resize" + extension;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        return newFilename;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
    public JsonObject call(JsonObject args) {// ！！！请勿修改此文件，此文件由脚本生成
        String imagepath = args.get("image_s3_path").getAsString();// ！！！请勿修改此文件，此文件由脚本生成
        int targetWidth = args.get("target_width").getAsInt();// ！！！请勿修改此文件，此文件由脚本生成
        int targetHeight = args.get("target_height").getAsInt();// ！！！请勿修改此文件，此文件由脚本生成
        try {// ！！！请勿修改此文件，此文件由脚本生成
            // Download the file from the bucket// ！！！请勿修改此文件，此文件由脚本生成
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()// ！！！请勿修改此文件，此文件由脚本生成
                        .bucket("serverless-bench")// ！！！请勿修改此文件，此文件由脚本生成
                        .object(imagepath)// ！！！请勿修改此文件，此文件由脚本生成
                        .build();// ！！！请勿修改此文件，此文件由脚本生成
            InputStream downloadedStream = minioClient.getObject(getObjectArgs);// ！！！请勿修改此文件，此文件由脚本生成
            ByteBuffer bf=readToByteBuffer(downloadedStream);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            byte[] resizedImage = resizeImage(bf.array(), targetWidth, targetHeight);// ！！！请勿修改此文件，此文件由脚本生成
            // ！！！请勿修改此文件，此文件由脚本生成
            ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedImage);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            minioClient.putObject(// ！！！请勿修改此文件，此文件由脚本生成
                    PutObjectArgs.builder()// ！！！请勿修改此文件，此文件由脚本生成
                            .bucket("serverless-bench")// ！！！请勿修改此文件，此文件由脚本生成
                            .object(renameFile(imagepath))// ！！！请勿修改此文件，此文件由脚本生成
                            .stream(inputStream, resizedImage.length, -1)// ！！！请勿修改此文件，此文件由脚本生成
                            .contentType("image/jpeg")// ！！！请勿修改此文件，此文件由脚本生成
                            .build()// ！！！请勿修改此文件，此文件由脚本生成
            );// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            JsonObject result = new JsonObject();// ！！！请勿修改此文件，此文件由脚本生成
            result.addProperty("resized_image", renameFile(imagepath));// ！！！请勿修改此文件，此文件由脚本生成
            return result;// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
        catch (Exception e) {// ！！！请勿修改此文件，此文件由脚本生成
            e.printStackTrace();// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
        return null;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
    byte[] resizeImage(byte[] imageData, int targetWidth, int targetHeight) {// ！！！请勿修改此文件，此文件由脚本生成
        try {// ！！！请勿修改此文件，此文件由脚本生成
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);// ！！！请勿修改此文件，此文件由脚本生成
            BufferedImage bufferedImage = ImageIO.read(bis);// ！！！请勿修改此文件，此文件由脚本生成
            bis.close();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            Image scaledImage = bufferedImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);// ！！！请勿修改此文件，此文件由脚本生成
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);// ！！！请勿修改此文件，此文件由脚本生成
            Graphics2D g2d = resizedImage.createGraphics();// ！！！请勿修改此文件，此文件由脚本生成
            g2d.drawImage(scaledImage, 0, 0, null);// ！！！请勿修改此文件，此文件由脚本生成
            g2d.dispose();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            ByteArrayOutputStream bos = new ByteArrayOutputStream();// ！！！请勿修改此文件，此文件由脚本生成
            ImageIO.write(resizedImage, "jpg", bos);// ！！！请勿修改此文件，此文件由脚本生成
            bos.close();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            return bos.toByteArray();// ！！！请勿修改此文件，此文件由脚本生成
        } catch (IOException e) {// ！！！请勿修改此文件，此文件由脚本生成
            e.printStackTrace();// ！！！请勿修改此文件，此文件由脚本生成
            return null;// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
}// ！！！请勿修改此文件，此文件由脚本生成
