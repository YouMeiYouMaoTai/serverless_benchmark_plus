package test.functions;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Parallel {
    private static final int DEFAULT_LOOP_TIME = 10000000;
    private static final int DEFAULT_PARALLEL_INDEX = 100;
    
    public JsonObject call(JsonObject args) {
        long start = System.currentTimeMillis();


        int loopTime = DEFAULT_LOOP_TIME;
        if (args.has("loopTime")) {
            loopTime = args.get("loopTime").getAsInt();
        }
        int parallelIndex = DEFAULT_PARALLEL_INDEX;
        if (args.has("parallelIndex")) {
            parallelIndex = args.get("parallelIndex").getAsInt();
        }

        
        ExecutorService executor = Executors.newFixedThreadPool(parallelIndex);
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < parallelIndex; i++) {
            futures.add(executor.submit(new AluTask(loopTime / parallelIndex)));
        }


        long end = System.currentTimeMillis();


        List<Long> results = new ArrayList<>();
        for (Future<Long> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }


        JsonObject result = new JsonObject();
        result.addProperty("startTime", start);
        result.addProperty("endTime", end);
        result.addProperty("execTime", end - start);
        result.addProperty("result", results.toString());
        return result;
    }

    static class AluTask implements Callable<Long> {
        private final long times;

        AluTask(long times) {
            this.times = times;
        }

        @Override
        public Long call() throws Exception {
            Random random = new Random();
            long temp = 0;
            int a = random.nextInt(91) + 10;
            int b = random.nextInt(91) + 10;

            for (long i = 0; i < times; i++) {
                switch ((int)(i % 4)) {
                    case 0:
                        temp = a + b;
                        break;
                    case 1:
                        temp = a - b;
                        break;
                    case 2:
                        temp = (long) a * b;
                        break;
                    case 3:
                        temp = a / b;
                        break;
                }
            }
            return temp;
        }
    }
}
