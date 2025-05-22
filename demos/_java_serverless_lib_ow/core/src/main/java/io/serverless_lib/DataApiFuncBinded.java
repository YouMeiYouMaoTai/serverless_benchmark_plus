package io.serverless_lib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.JsonObject;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;




public class DataApiFuncBinded {
    private MinioClient minioClient;
    private String bucketName = "serverless-bench";
    private String func_name;


    public DataApiFuncBinded(String func_name, JsonObject args, String use_minio) {
        // this.srcTaskId = FnTaskId.newBuilder()
        //     .setCallNodeId(args.get("srcTaskCalledBy").getAsInt())
        //     .setTaskId(args.get("srcTaskId").getAsInt()).build();

        this.func_name = func_name;

        // 初始化 MinIO 客户端，使用与 DataApi 相同的解析逻辑
        if (use_minio == null || use_minio.isEmpty()) {
            throw new RuntimeException("MinIO connection string is required");
        } else {
            try {
                // 解析MinIO连接字符串 (格式示例: "http://localhost:9000,accessKey,secretKey,bucketName")
                String[] parts = use_minio.split(",");
                String endpoint = parts[0];
                String accessKey = parts.length > 1 ? parts[1] : "";
                String secretKey = parts.length > 2 ? parts[2] : "";
                this.bucketName = parts.length > 3 ? parts[3] : "serverless-bench";
                
                this.minioClient = MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
                
                // 确保存储桶存在
                boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
                if (!found) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                }
                
                System.out.println("DataApiFuncBinded initialized in MinIO mode: " + endpoint);
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize MinIO client: " + e.getMessage(), e);
            }
        }
    }

    // 生成对象键
    private String generateObjectKey(String key) {
        return key;
    }

    // 简化后只处理单个值
    public void put(String key, byte[][] value) throws IOException {
        try {
            String objectKey = generateObjectKey(key);
            ByteArrayInputStream bais = new ByteArrayInputStream(value[0]);
            
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(bais, value.length, -1)
                    .build()
            );
        } catch (MinioException e) {
            throw new IOException("Error putting object to MinIO"+e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Unexpected error during MinIO put operation"+e.getMessage(), e);
        }
    }
    
    // 简化后不再支持获取特定索引，只返回单个值
    public Map<Integer, byte[]> get(String key, int[] item_idxs) throws IOException {
        try {
            String objectKey = generateObjectKey(key);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            HashMap<Integer, byte[]> result = new HashMap<>();
            result.put(item_idxs[0], baos.toByteArray());
            return result;
            // return baos.toByteArray();

        } catch (MinioException e) {
            // e.printStackTrace();
            throw new IOException("Error getting object ("+key+") from MinIO:"+e.getMessage(), e);
        } catch (Exception e) {
            // e.printStackTrace();
            // throw new IOException("Unexpected error during MinIO get operation:"+e.getMessage(), e);
            throw new IOException("Error getting object ("+key+") from MinIO:"+e.getMessage(), e);
        }
    }
    
    // 简化后只返回单个值
    public byte[][] delete(String key) throws IOException {
        try {
            String objectKey = generateObjectKey(key);
            
            // 先获取对象，然后删除
            int[] item_idxs = {0};
            byte[] data = get(key, item_idxs).get(item_idxs[0]);
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build()
            );
            
            byte[][] result = new byte[1][];
            result[0] = data;
            return result;
        } catch (MinioException e) {
            throw new IOException("Error deleting object from MinIO", e);
        } catch (Exception e) {
            throw new IOException("Unexpected error during MinIO delete operation", e);
        }
    }
}