package test.functions;

import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.io.InputStream;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.PutObjectArgs;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import com.google.gson.JsonParser;
import io.serverless_lib.DataApiFuncBinded;

public class Resize {
    // Initialize Minio Client
    // MinioClient minioClient =
    //         MinioClient.builder()
    //                 .endpoint("http://192.168.31.96:9009")
    //                 .credentials("minioadmin", "minioadmin123")
    //                 .build();

    // 辅助方法：将输入流读取到 ByteBuffer
    private static ByteBuffer readToByteBuffer(InputStream inputStream) throws IOException {
        // Start with a default buffer size
        ByteBuffer byteBuffer = ByteBuffer.allocate(10000);

        // Read data from InputStream and write to ByteBuffer
        byte[] buffer = new byte[1000];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            // Ensure there's enough space in the ByteBuffer
            if (byteBuffer.remaining() < bytesRead) {
                // Expand ByteBuffer
                byteBuffer = expandBuffer(byteBuffer, bytesRead);
            }
            byteBuffer.put(buffer, 0, bytesRead);
        }

        // Reset ByteBuffer's position, ready for reading
        byteBuffer.flip();

        return byteBuffer;
    }
    // Method to expand the ByteBuffer
    private static ByteBuffer expandBuffer(ByteBuffer byteBuffer, int additionalCapacity) {
        // Calculate new capacity
        int newCapacity = byteBuffer.capacity() + Math.max(additionalCapacity, byteBuffer.capacity());
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        byteBuffer.flip(); // Prepare buffer for copying
        newBuffer.put(byteBuffer);
        return newBuffer;
    }
    public static String renameFile(String originalFilename) {
        // // Find the last dot in the filename
        // int dotIndex = originalFilename.lastIndexOf('.');
        
        
        // String baseName;
        // String extension;
        // // If there is no dot, or it's the first character, handle it
        // if (dotIndex == -1 || dotIndex == 0) {
        //     // allow no tail
        //     baseName = originalFilename;
        //     extension = "";
        // }
        // else{
        //     baseName = originalFilename.substring(0, dotIndex);
        //     extension = originalFilename.substring(dotIndex);
        // }

        // // Extract the base name (without extension)
        // String baseName = originalFilename.substring(0, dotIndex);
        // // Extract the extension
        // String extension = originalFilename.substring(dotIndex);

        // Construct the new filename
        String newFilename = "_resized_"+ originalFilename;

        return newFilename;
    }
    public JsonObject call(JsonObject args) {
        /// waverless embbed data storage
        /// - binded request data and big data in DataSet
        ///   https://fvd360f8oos.feishu.cn/wiki/M4ubwJkvcichuHkiGhjc0miHn5f#share-F0WBdFFhdop2ELxS3ZlcHWvZnD8
        String imagepath=null;
        int targetWidth=0;
        int targetHeight=0;
        String useMinio=null;
        DataApiFuncBinded dataApi = null;

        // Debug function args
        System.out.println("------------ debug -------------");
        System.out.println("args: " + args.toString());
        System.out.println("--------------------------------");

        if (args.get("trigger_data_key") != null) {
            dataApi = new DataApiFuncBinded("resize", args, ""); // "" pass empty minio config
            // request json str
            int[] item_idxs = {0};
            String trigger_data_key = args.get("trigger_data_key").getAsString();
            System.out.println("try to get trigger_data_key: " + trigger_data_key);
            String requestJsonStr = null;
            try{
                requestJsonStr = new String(dataApi.get(trigger_data_key,item_idxs).get(0), "UTF-8");
                args=new JsonParser().parse(requestJsonStr).getAsJsonObject();
            }catch(Exception e){
                // print string trying to parse
                System.out.println("trying to parse: '" + requestJsonStr + "'");
                e.printStackTrace();
                JsonObject errResp=new JsonObject();
                errResp.addProperty("error", e.getMessage());
                return errResp;
                // throw new Exception("Failed to get trigger_data_key: " + trigger_data_key);
            }

            imagepath = args.get("image_s3_path").getAsString();
            targetWidth = args.get("target_width").getAsInt();
            targetHeight = args.get("target_height").getAsInt();
            useMinio = "minio";
        }else{
            imagepath = args.get("image_s3_path").getAsString();
            targetWidth = args.get("target_width").getAsInt();
            targetHeight = args.get("target_height").getAsInt();
            useMinio = args.get("use_minio").getAsString();
            // dataApi.init(useMinio);
            dataApi=new DataApiFuncBinded("resize", args, useMinio);
        }

        // print useMinio
        System.out.println("--------------------------------");
        System.out.println("imagepath: " + imagepath);
        System.out.println("targetWidth: " + targetWidth);
        System.out.println("targetHeight: " + targetHeight);
        System.out.println("useMinio: " + useMinio);
        System.out.println("--------------------------------");

        // 使用静态方法获取DataApi实例
        

        JsonObject result = new JsonObject();
        try {
            // ByteBuffer bf=readToByteBuffer(downloadedStream);
            // GetObjectArgs getObjectArgs = GetObjectArgs.builder()
            //             .bucket("serverless-bench")
            //             .object(imagepath)
            //             .build();
            int[] item_idxs = {1};
            byte[] imageData = dataApi.get(imagepath, item_idxs).get(1);
            // ByteBuffer bf=readToByteBuffer(downloadedStream);
            if (imageData==null){
                JsonObject errResp=new JsonObject();
                errResp.addProperty("error", "imageData is null");
                return errResp;
            }

            byte[] resizedImage = resizeImage(imageData, targetWidth, targetHeight);
            
            // ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedImage);
            // ByteArrayInputStream inputStream = new ByteArrayInputStream(resizedImage);

            byte[][] resizedImageData = {resizedImage};
            dataApi.put(renameFile(imagepath), resizedImageData);
            // minioClient.putObject(
            //         PutObjectArgs.builder()
            //                 .bucket("serverless-bench")
            //                 .object(renameFile(imagepath))
            //                 .stream(inputStream, resizedImage.length, -1)
            //                 .contentType("image/jpeg")
            //                 .build()
            // );


            
            result.addProperty("resized_image", renameFile(imagepath));
        }
        catch (Exception e) {
            e.printStackTrace();
            result.addProperty("error", e.getMessage());
        }
        return result;
    }
    byte[] resizeImage(byte[] imageData, int targetWidth, int targetHeight) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage bufferedImage = ImageIO.read(bis);
            bis.close();

            Image scaledImage = bufferedImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", bos);
            bos.close();

            return bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
