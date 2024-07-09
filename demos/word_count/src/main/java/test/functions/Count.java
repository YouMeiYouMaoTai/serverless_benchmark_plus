package test.functions;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Count {

    private static final int BUFFER_SIZE = 1024 * 1024; // 1MB
    private static final Path FILE_PATH = Paths.get("random_words.txt");
    private static final Map<String, byte[]> KV_STORE = new ConcurrentHashMap<>();

    public void splitFile() throws IOException {
        try (FileChannel fileChannel = FileChannel.open(FILE_PATH)) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            int sliceId = 0;

            while (fileChannel.read(buffer) != -1) {
                // Flip the buffer for reading
                buffer.flip();
                byte[] slice = new byte[buffer.limit()];
                buffer.get(slice);

                // Find the last newline character
                int lastNewLineIndex = findLastNewLine(slice);
                if (lastNewLineIndex != -1) {
                    // Store the slice in KV_STORE
                    String key = "wordcount_slice_" + sliceId++;
                    KV_STORE.put(key, Arrays.copyOf(slice, lastNewLineIndex + 1));
                } else {
                    // If no newline character found, compact the buffer
                    buffer.compact();
                    continue;
                }

                // Move remaining data to the beginning of the buffer
                if (lastNewLineIndex < slice.length - 1) {
                    int remainingSize = slice.length - lastNewLineIndex - 1;
                    System.arraycopy(slice, lastNewLineIndex + 1, buffer.array(), 0, remainingSize);
                    buffer.position(remainingSize);
                    buffer.limit(BUFFER_SIZE);
                }
            }
            buffer.clear();
        }
    }

    private int findLastNewLine(byte[] slice) {
        for (int i = slice.length - 1; i >= 0; i--) {
            if (slice[i] == '\n') {
                return i;
            }
        }
        return -1;
    }

    public void handleOneSlice(String key) {
        byte[] value = KV_STORE.get(key);
        if (value != null) {
            System.out.println("handleOneSlice k " + key + " v " + new String(value));
        } else {
            System.err.println("No slice found for key: " + key);
        }
    }

    public JsonObject call(JsonObject args) {
        JsonObject result = new JsonObject();
        try {
            // Split the file
            splitFile();

            // Iterate through KV_STORE and add slices to result
            for (Map.Entry<String, byte[]> entry : KV_STORE.entrySet()) {
                String key = entry.getKey();
                String value = new String(entry.getValue());
                result.addProperty(key, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
            result.addProperty("error", "An error occurred while processing the file: " + e.getMessage());
        }
        return result;
    }
}
