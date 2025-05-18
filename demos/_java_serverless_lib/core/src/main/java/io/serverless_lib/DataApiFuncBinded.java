package io.serverless_lib;

import java.io.IOException;
import java.util.Map;
import com.google.gson.JsonObject;
import process_rpc_proto.ProcessRpcProto.FnTaskId;

public class DataApiFuncBinded {
    private DataApi dataApi;
    private String func_name;
    private FnTaskId srcTaskId;

    public DataApiFuncBinded(String func_name, JsonObject args,String use_minio) {
        this.dataApi = DataApi.getInstance();
        this.srcTaskId = FnTaskId.newBuilder()
            .setCallNodeId(args.get("srcTaskCalledBy").getAsInt())
            .setTaskId(args.get("srcTaskId").getAsInt()).build();

        // init
        dataApi.init(use_minio);

        this.func_name = func_name;
    }

    public void put(String key, byte[][] value) throws IOException {
        dataApi.put(srcTaskId, func_name, key, value);
    }
    
    public Map<Integer, byte[]> get(String key, int[] item_idxs) throws IOException {
        return dataApi.get(srcTaskId, func_name, key, item_idxs);
    }
    
    public byte[][] delete(String key) throws IOException {
        return dataApi.delete(srcTaskId, func_name, key);
    }
}