package test.functions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.serverless_lib.DataApiFuncBinded;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Count {

    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final Map<String, byte[]> KV_STORE = new ConcurrentHashMap<>();
    
    // 移除直接MinioClient初始化，将通过DataApiFuncBinded处理
    // MinioClient minioClient = 
    //     MinioClient.builder()
    //         .endpoint("http://192.168.31.96:9009")
    //         .credentials("minioadmin", "minioadmin123")
    //         .build();

    public void splitFile(byte[] fileData, AtomicInteger sliceIdCounter) throws IOException {
        KV_STORE.clear();
        
        int totalLength = fileData.length;
        int offset = 0;

        while (offset < totalLength) {
            int chunkSize = Math.min(BUFFER_SIZE, totalLength - offset);
            byte[] slice = new byte[chunkSize];
            System.arraycopy(fileData, offset, slice, 0, chunkSize);
            
            String key = "wordcount_slice_" + sliceIdCounter.getAndIncrement();
            KV_STORE.put(key, slice);
            System.out.println("Stored slice: " + key);
            
            offset += chunkSize;
        }
    }

    public JsonObject call(JsonObject args) {
        JsonObject result = new JsonObject();
        AtomicInteger sliceIdCounter = new AtomicInteger(0);
        
        String textS3Path = null;
        String useMinio = null;
        DataApiFuncBinded dataApi = null;
        
        try {
            System.out.println("Received arguments: " + args.toString());
            
            // 处理参数，与Resize.java类似
            if (args.get("trigger_data_key") != null) {
                dataApi = new DataApiFuncBinded("count", args, ""); // 空字符串表示不使用Minio配置
                int[] item_idxs = {0};
                String trigger_data_key = args.get("trigger_data_key").getAsString();
                System.out.println("Try to get trigger_data_key: " + trigger_data_key);
                String requestJsonStr = null;
                try {
                    requestJsonStr = new String(dataApi.get(trigger_data_key, item_idxs).get(0), "UTF-8");
                    args = new JsonParser().parse(requestJsonStr).getAsJsonObject();
                } catch (Exception e) {
                    System.out.println("Trying to parse: '" + requestJsonStr + "'");
                    e.printStackTrace();
                    result.addProperty("error", e.getMessage());
                    return result;
                }
                
                textS3Path = args.get("text_s3_path").getAsString();
                useMinio = "minio";
            } else {
                textS3Path = args.get("text_s3_path").getAsString();
                useMinio = args.has("use_minio") ? args.get("use_minio").getAsString() : "";
                dataApi = new DataApiFuncBinded("count", args, useMinio);
            }
            
            System.out.println("--------------------------------");
            System.out.println("textS3Path: " + textS3Path);
            System.out.println("useMinio: " + useMinio);
            System.out.println("--------------------------------");
            
            // 使用dataApi获取文件数据
            System.out.println("Downloading file: " + textS3Path);
            int[] item_idxs = {1};
            byte[] fileData = dataApi.get(textS3Path, item_idxs).get(1);
            
            if (fileData == null) {
                result.addProperty("error", "File data is null");
                return result;
            }
            
            System.out.println("File downloaded successfully, size: " + fileData.length + " bytes");
            
            // 分割文件
            splitFile(fileData, sliceIdCounter);
            System.out.println("File split into " + KV_STORE.size() + " slices");
            
            // 上传每个分片
            for (Map.Entry<String, byte[]> entry : KV_STORE.entrySet()) {
                String key = entry.getKey();
                byte[] value = entry.getValue();
                System.out.println("Uploading slice: " + key);
                
                // 使用dataApi上传分片
                byte[][] sliceData = {value};
                dataApi.put(key, sliceData);
                
                // 将每个分片键添加到结果中
                result.addProperty("slice_" + key, key);
                System.out.println("Slice uploaded successfully: " + key);
            }
            
            System.out.println("All slices uploaded successfully");
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.addProperty("error", "An error occurred while processing the file: " + e.getMessage());
            return result;
        }
    }
}
