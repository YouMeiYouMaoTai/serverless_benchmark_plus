package test.functions;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

import io.serverless_lib.KvBatch;

public class TestJavaKv {
    private KvBatch kvBatch;

    public void InitKvBatch(String app, String func, int taskId) {
        this.kvBatch = new KvBatch(app, func, taskId);
    }

    public JsonObject call(JsonObject args) {
        InitKvBatch("javakv-test", "javakv-test-func", 9999);

        kvBatch.addGetRequest("get_start".getBytes(), "get_end".getBytes());
        kvBatch.addPutRequest("put_key".getBytes(), "put_value".getBytes());
        kvBatch.addDeleteRequest("delete_start".getBytes(), "delete_end".getBytes());
        kvBatch.addLockRequest(true, Arrays.asList(0), "read_lock_start".getBytes(), "read_lock_end".getBytes());
        kvBatch.addLockRequest(false, Arrays.asList(1), "write_lock_start".getBytes(), "write_lock_end".getBytes());

        try {
            kvBatch.sendBatch();

            // 等待5s，等待服务端处理完成
            Thread.sleep(5000);

            JsonObject result = new JsonObject();

            System.out.println("Get result: " + kvBatch.getResult().toString());

            result.addProperty("KvBatch result", kvBatch.getResult().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
}
