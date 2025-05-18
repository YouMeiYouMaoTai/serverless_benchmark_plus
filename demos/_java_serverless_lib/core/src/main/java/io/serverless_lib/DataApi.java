package io.serverless_lib;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.netty.buffer.ByteBuf;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

// Import the protocol buffer classes
import process_rpc_proto.ProcessRpcProto.KeyRange;
import process_rpc_proto.ProcessRpcProto.KvPair;
import process_rpc_proto.ProcessRpcProto.KvRequest;
import process_rpc_proto.ProcessRpcProto.KvResponse;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import process_rpc_proto.ProcessRpcProto.FnTaskId;

/**
 * DataApi用于处理数据存储，支持内存模式和MinIO模式
 */
public class DataApi implements InitializingBean {
    
    // 静态实例，可以在任何地方访问
    private static DataApi instance;

    @Autowired
    private UdsBackend udsBackend;
    
    /**
     * 获取DataApi实例的静态方法
     * @return DataApi实例
     */
    public static DataApi getInstance() {
        if (instance == null) {
            // 如果实例为null，创建一个新实例
            synchronized (DataApi.class) {
                if (instance == null) {
                    instance = new DataApi();
                }
            }
        }
        return instance;
    }
    
    /**
     * Spring Bean初始化后执行的方法，设置静态实例
     */
    @Override
    public void afterPropertiesSet() {
        instance = this;
        System.out.println("DataApi静态实例已设置");
    }
    
    private volatile boolean initialized = false;
    private Map<String, byte[]> memoryStorage;
    private MinioClient minioClient;
    private String storageBucket;
    private String storageMode;
    
    /**
     * 延迟初始化函数，使用线程安全的双重检查锁定模式
     * @param use_minio MinIO连接字符串，为空时使用内存Map存储
     */
    public void init(String use_minio) {
        // 第一次检查，避免每次调用都加锁
        if (initialized) {
            return;
        }
        
        // 使用同步块保护初始化过程
        synchronized (this) {
            // 二次检查，防止其他线程已经完成了初始化
            if (initialized) {
                return;
            }
            
            if (use_minio == null || use_minio.isEmpty()) {
                // 使用内存Map模式
                memoryStorage = new ConcurrentHashMap<>();
                storageMode = "waverless_storage";
                System.out.println("DataApi initialized in memory mode");
            } else {
                // 连接到MinIO
                try {
                    // 解析MinIO连接字符串 (格式示例: "http://localhost:9000,accessKey,secretKey,bucketName")
                    String[] parts = use_minio.split(",");
                    String endpoint = parts[0];
                    String accessKey = parts.length > 1 ? parts[1] : "";
                    String secretKey = parts.length > 2 ? parts[2] : "";
                    storageBucket = parts.length > 3 ? parts[3] : "serverless-bench";
                    
                    minioClient = MinioClient.builder()
                            .endpoint(endpoint)
                            .credentials(accessKey, secretKey)
                            .build();
                    
                    // 确保存储桶存在
                    boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(storageBucket).build());
                    if (!found) {
                        minioClient.makeBucket(MakeBucketArgs.builder().bucket(storageBucket).build());
                    }
                    
                    storageMode = "minio";
                    System.out.println("DataApi initialized in MinIO mode: " + endpoint);
                } catch (Exception e) {
                    // 连接MinIO失败，回退到内存模式
                    memoryStorage = new ConcurrentHashMap<>();
                    storageMode = "waverless_storage";
                    System.err.println("Failed to connect to MinIO, fallback to memory mode: " + e.getMessage());
                }
            }
            
            // 最后设置初始化标志
            initialized = true;
        }
    }
    
    /**
     * 保存数据
     * @param func_name 函数名
     * @param key 键
     * @param values 二维字节数组值
     * @throws IOException 操作失败时抛出异常
     */
    public void put(FnTaskId srcTaskId,String func_name, String key, byte[][] values) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("DataApi not initialized. Call init() first.");
        }
        
        if ("waverless_storage".equals(storageMode)) {
            try {
                // Build the KvPair using protobuf builder
                KvPair.Builder kvBuilder = KvPair.newBuilder()
                    .setKey(ByteString.copyFrom(key.getBytes()));
                
                // Add each value as a ByteString
                for (byte[] value : values) {
                    kvBuilder.addValues(ByteString.copyFrom(value));
                }
                
                // Build the KvPutRequest using protobuf builder
                KvRequest.KvPutRequest.Builder putBuilder = KvRequest.KvPutRequest.newBuilder()
                    .setSrcTaskId(srcTaskId)
                    .setKv(kvBuilder);
                
                // Build the KvRequest using protobuf builder
                KvRequest req = KvRequest.newBuilder()
                    .setSet(putBuilder)
                    .build();

                KvResponse response = (KvResponse) udsBackend.sendRpc(
                    udsBackend.newRpcReqUdsPack(
                        req,
                        func_name
                    ),
                    60,
                    TimeUnit.SECONDS
                );
            } catch (Exception e) {
                throw new IOException("Failed to put data to waverless storage: " + e.getMessage(), e);
            }
        } else {
            try {
                // MinIO only supports single values, so we use the first value
                if (values == null || values.length == 0) {
                    throw new IllegalArgumentException("No values provided for MinIO storage");
                }
                
                byte[] singleValue = values[0];
                // 将字节数组上传到MinIO
                ByteArrayInputStream bais = new ByteArrayInputStream(singleValue);
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(storageBucket)
                        .object(key)
                        .stream(bais, singleValue.length, -1)
                        .contentType("application/octet-stream")
                        .build()
                );
                bais.close();
            } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IOException("Failed to put object to MinIO: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * 获取数据
     * @param func_name 函数名
     * @param key 键
     * @param item_idxs 数据项索引
     * @return data item idx -> byte[]
     * @throws IOException 操作失败时抛出异常
     */
    public Map<Integer, byte[]> get(FnTaskId srcTaskId,String func_name, String key, int[] item_idxs) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("DataApi not initialized. Call init() first.");
        }
        
        Map<Integer, byte[]> result = new HashMap<>();
        
        if ("waverless_storage".equals(storageMode)) {
            try {
                // Create KeyRange for the KvGetRequest
                KeyRange.Builder rangeBuilder = KeyRange.newBuilder().setStart(ByteString.copyFromUtf8(key));
                
                // Build the KvGetRequest
                KvRequest.KvGetRequest.Builder getBuilder = KvRequest.KvGetRequest.newBuilder()
                    .setSrcTaskId(srcTaskId)
                    .setRange(rangeBuilder);
                
                // Add all indexes
                for (int idx : item_idxs) {
                    getBuilder.addIdxs(idx);
                }
                
                // Build the final KvRequest
                KvRequest req = KvRequest.newBuilder()
                    .setGet(getBuilder)
                    .build();
                
                KvResponse kvResponse = (KvResponse) udsBackend.sendRpc(
                    udsBackend.newRpcReqUdsPack(
                        req,
                        func_name
                    ),
                    60,
                    TimeUnit.SECONDS
                );
                
                
                if (kvResponse.hasGet()) {
                    KvResponse.KvGetResponse getResponse = kvResponse.getGet();
                    
                    // Check if response size matches request
                    if (getResponse.getIdxsCount() != item_idxs.length) {
                        throw new IOException("Failed to get object " + key + " from storage: response count doesn't match required item_idxs count");
                    }
                    
                    // Process the returned values
                    for (int i = 0; i < getResponse.getIdxsCount(); i++) {
                        int idx = getResponse.getIdxs(i);
                        ByteString value = getResponse.getValues(i);
                        result.put(idx, value.toByteArray());
                    }
                } else {
                    throw new IOException("Failed to get object " + key + " from storage: response doesn't contain get field");
                }
            } catch (Exception e) {
                throw new IOException("Failed to get data from waverless storage: " + e.getMessage(), e);
            }
            return result;
        } else {
            try {
                // 从MinIO获取对象
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                        .bucket(storageBucket)
                        .object(key)
                        .build()
                );
                
                // 读取字节到输出流
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = stream.read(buffer)) > 0) {
                    baos.write(buffer, 0, bytesRead);
                }
                
                stream.close();
                byte[] data = baos.toByteArray();
                baos.close();
                
                // 我们只返回索引0，因为我们只支持单元素数组
                if (item_idxs == null || arrayContains(item_idxs, 0)) {
                    result.put(0, data);
                }
                
                return result;
            } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IOException("Failed to get object " + key + " from MinIO: " + e.getMessage(), e);
            }
        }
    }
    
    // 辅助方法，检查数组是否包含某个值
    private boolean arrayContains(int[] array, int value) {
        if (array == null) return false;
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }


    /**
     * 删除数据
     * @param func_name 函数名
     * @param key 要删除的键
     * @throws IOException 操作失败时抛出异常
     */
    public byte[][] delete(FnTaskId srcTaskId,String func_name, String key) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("DataApi not initialized. Call init() first.");
        }
        
        if ("waverless_storage".equals(storageMode)) {
            try {
                // Create KeyRange for the KvDeleteRequest
                KeyRange.Builder rangeBuilder = KeyRange.newBuilder().setStart(ByteString.copyFromUtf8(key));
                
                // Build the KvDeleteRequest
                KvRequest.KvDeleteRequest.Builder deleteBuilder = KvRequest.KvDeleteRequest.newBuilder()
                    .setSrcTaskId(srcTaskId)
                    .setRange(rangeBuilder);
                
                // Build the final KvRequest
                KvRequest req = KvRequest.newBuilder()
                    .setDelete(deleteBuilder)
                    .build();
                
                KvResponse kvResponse = (KvResponse) udsBackend.sendRpc(
                    udsBackend.newRpcReqUdsPack(
                        req,
                        func_name
                    ),
                    60,
                    TimeUnit.SECONDS
                );


                if (kvResponse.hasPutOrDel()) {
                    KvResponse.KvPutOrDelResponse putOrDelResponse = kvResponse.getPutOrDel();
                    byte[][] result = new byte[putOrDelResponse.getKv().getValuesCount()][];
                    for (int i = 0; i < putOrDelResponse.getKv().getValuesCount(); i++) {
                        result[i] = putOrDelResponse.getKv().getValues(i).toByteArray();
                    }
                    return result;
                } else {
                    throw new IOException("Failed to delete object " + key + " from storage: response doesn't contain putOrDel field");
                }
                
            } catch (Exception e) {
                throw new IOException("Failed to delete data from waverless storage: " + e.getMessage(), e);
            }
        } else {
            try {
                // 从MinIO删除对象
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(storageBucket)
                        .object(key)
                        .build()
                );
            } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
                throw new IOException("Failed to delete object from MinIO: " + e.getMessage(), e);
            }

            return null;
        }
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 获取当前存储模式
     */
    public String getStorageMode() {
        return storageMode;
    }
} 