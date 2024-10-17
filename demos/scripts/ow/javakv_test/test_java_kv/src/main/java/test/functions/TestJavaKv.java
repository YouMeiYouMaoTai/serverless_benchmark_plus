package test.functions;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
import java.util.Arrays;// ！！！请勿修改此文件，此文件由脚本生成
import java.util.List;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
import com.google.gson.JsonObject;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
import io.serverless_lib.KvBatch;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
public class TestJavaKv {// ！！！请勿修改此文件，此文件由脚本生成
    private KvBatch kvBatch;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    public void InitKvBatch(String app, String func, int taskId) {// ！！！请勿修改此文件，此文件由脚本生成
        this.kvBatch = new KvBatch(app, func, taskId);// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    public JsonObject call(JsonObject args) {// ！！！请勿修改此文件，此文件由脚本生成
        InitKvBatch("javakv-test", "javakv-test-func", 9999);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        kvBatch.addGetRequest("get_start".getBytes(), "get_end".getBytes());// ！！！请勿修改此文件，此文件由脚本生成
        kvBatch.addPutRequest("put_key".getBytes(), "put_value".getBytes());// ！！！请勿修改此文件，此文件由脚本生成
        kvBatch.addDeleteRequest("delete_start".getBytes(), "delete_end".getBytes());// ！！！请勿修改此文件，此文件由脚本生成
        kvBatch.addLockRequest(true, Arrays.asList(0), "read_lock_start".getBytes(), "read_lock_end".getBytes());// ！！！请勿修改此文件，此文件由脚本生成
        kvBatch.addLockRequest(false, Arrays.asList(1), "write_lock_start".getBytes(), "write_lock_end".getBytes());// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
        try {// ！！！请勿修改此文件，此文件由脚本生成
            kvBatch.sendBatch();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            // 等待5s，等待服务端处理完成// ！！！请勿修改此文件，此文件由脚本生成
            Thread.sleep(5000);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            JsonObject result = new JsonObject();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            System.out.println("Get result: " + kvBatch.getResult().toString());// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            result.addProperty("KvBatch result", kvBatch.getResult().toString());// ！！！请勿修改此文件，此文件由脚本生成
        } catch (Exception e) {// ！！！请勿修改此文件，此文件由脚本生成
            e.printStackTrace();// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
        // ！！！请勿修改此文件，此文件由脚本生成
        return null;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
    // ！！！请勿修改此文件，此文件由脚本生成
}// ！！！请勿修改此文件，此文件由脚本生成
