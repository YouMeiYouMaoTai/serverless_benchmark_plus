package test.functions;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
import com.google.gson.JsonObject;// ！！！请勿修改此文件，此文件由脚本生成
import java.io.IOException;// ！！！请勿修改此文件，此文件由脚本生成
import java.nio.ByteBuffer;// ！！！请勿修改此文件，此文件由脚本生成
import java.nio.channels.FileChannel;// ！！！请勿修改此文件，此文件由脚本生成
import java.nio.file.Path;// ！！！请勿修改此文件，此文件由脚本生成
import java.nio.file.Paths;// ！！！请勿修改此文件，此文件由脚本生成
import java.util.Arrays;// ！！！请勿修改此文件，此文件由脚本生成
import java.util.Map;// ！！！请勿修改此文件，此文件由脚本生成
import java.util.concurrent.ConcurrentHashMap;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
public class Count {// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB// ！！！请勿修改此文件，此文件由脚本生成
    private static final Path FILE_PATH = Paths.get("random_words.txt");// ！！！请勿修改此文件，此文件由脚本生成
    private static final Map<String, byte[]> KV_STORE = new ConcurrentHashMap<>();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    public void splitFile() throws IOException {// ！！！请勿修改此文件，此文件由脚本生成
        try (FileChannel fileChannel = FileChannel.open(FILE_PATH)) {// ！！！请勿修改此文件，此文件由脚本生成
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);// ！！！请勿修改此文件，此文件由脚本生成
            int sliceId = 0;// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            while (fileChannel.read(buffer) != -1) {// ！！！请勿修改此文件，此文件由脚本生成
                // Flip the buffer for reading// ！！！请勿修改此文件，此文件由脚本生成
                buffer.flip();// ！！！请勿修改此文件，此文件由脚本生成
                byte[] slice = new byte[buffer.limit()];// ！！！请勿修改此文件，此文件由脚本生成
                buffer.get(slice);// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
                // Find the last newline character// ！！！请勿修改此文件，此文件由脚本生成
                int lastNewLineIndex = findLastNewLine(slice);// ！！！请勿修改此文件，此文件由脚本生成
                if (lastNewLineIndex != -1) {// ！！！请勿修改此文件，此文件由脚本生成
                    // Store the slice in KV_STORE// ！！！请勿修改此文件，此文件由脚本生成
                    String key = "wordcount_slice_" + sliceId++;// ！！！请勿修改此文件，此文件由脚本生成
                    KV_STORE.put(key, Arrays.copyOf(slice, lastNewLineIndex + 1));// ！！！请勿修改此文件，此文件由脚本生成
                } else {// ！！！请勿修改此文件，此文件由脚本生成
                    // If no newline character found, compact the buffer// ！！！请勿修改此文件，此文件由脚本生成
                    buffer.compact();// ！！！请勿修改此文件，此文件由脚本生成
                    continue;// ！！！请勿修改此文件，此文件由脚本生成
                }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
                // Move remaining data to the beginning of the buffer// ！！！请勿修改此文件，此文件由脚本生成
                if (lastNewLineIndex < slice.length - 1) {// ！！！请勿修改此文件，此文件由脚本生成
                    int remainingSize = slice.length - lastNewLineIndex - 1;// ！！！请勿修改此文件，此文件由脚本生成
                    System.arraycopy(slice, lastNewLineIndex + 1, buffer.array(), 0, remainingSize);// ！！！请勿修改此文件，此文件由脚本生成
                    buffer.position(remainingSize);// ！！！请勿修改此文件，此文件由脚本生成
                    buffer.limit(BUFFER_SIZE);// ！！！请勿修改此文件，此文件由脚本生成
                }// ！！！请勿修改此文件，此文件由脚本生成
            }// ！！！请勿修改此文件，此文件由脚本生成
            buffer.clear();// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    private int findLastNewLine(byte[] slice) {// ！！！请勿修改此文件，此文件由脚本生成
        for (int i = slice.length - 1; i >= 0; i--) {// ！！！请勿修改此文件，此文件由脚本生成
            if (slice[i] == '\n') {// ！！！请勿修改此文件，此文件由脚本生成
                return i;// ！！！请勿修改此文件，此文件由脚本生成
            }// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
        return -1;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    public void handleOneSlice(String key) {// ！！！请勿修改此文件，此文件由脚本生成
        byte[] value = KV_STORE.get(key);// ！！！请勿修改此文件，此文件由脚本生成
        if (value != null) {// ！！！请勿修改此文件，此文件由脚本生成
            System.out.println("handleOneSlice k " + key + " v " + new String(value));// ！！！请勿修改此文件，此文件由脚本生成
        } else {// ！！！请勿修改此文件，此文件由脚本生成
            System.err.println("No slice found for key: " + key);// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
    public JsonObject call(JsonObject args) {// ！！！请勿修改此文件，此文件由脚本生成
        JsonObject result = new JsonObject();// ！！！请勿修改此文件，此文件由脚本生成
        try {// ！！！请勿修改此文件，此文件由脚本生成
            // Split the file// ！！！请勿修改此文件，此文件由脚本生成
            splitFile();// ！！！请勿修改此文件，此文件由脚本生成
// ！！！请勿修改此文件，此文件由脚本生成
            // Iterate through KV_STORE and add slices to result// ！！！请勿修改此文件，此文件由脚本生成
            for (Map.Entry<String, byte[]> entry : KV_STORE.entrySet()) {// ！！！请勿修改此文件，此文件由脚本生成
                String key = entry.getKey();// ！！！请勿修改此文件，此文件由脚本生成
                String value = new String(entry.getValue());// ！！！请勿修改此文件，此文件由脚本生成
                result.addProperty(key, value);// ！！！请勿修改此文件，此文件由脚本生成
            }// ！！！请勿修改此文件，此文件由脚本生成
        } catch (IOException e) {// ！！！请勿修改此文件，此文件由脚本生成
            e.printStackTrace();// ！！！请勿修改此文件，此文件由脚本生成
            result.addProperty("error", "An error occurred while processing the file: " + e.getMessage());// ！！！请勿修改此文件，此文件由脚本生成
        }// ！！！请勿修改此文件，此文件由脚本生成
        return result;// ！！！请勿修改此文件，此文件由脚本生成
    }// ！！！请勿修改此文件，此文件由脚本生成
}// ！！！请勿修改此文件，此文件由脚本生成
