package test.functions;


import com.google.gson.JsonObject;

public class Sequential {
    private static final int DEFAULT_LOOP_TIME = 10000000;

    public JsonObject call(JsonObject args) {
        long start = System.currentTimeMillis();


        int loopTime = DEFAULT_LOOP_TIME;
        if (args.has("loopTime")) {
            loopTime = args.get("loopTime").getAsInt();
        }

        
        double temp = singleAlu(loopTime);

        
        long end = System.currentTimeMillis();
        
        
        JsonObject result = new JsonObject();
        result.addProperty("startTime", start);
        result.addProperty("endTime", end);
        result.addProperty("execTime", end - start);
        result.addProperty("result", temp);
        return result;
    }

    private static Double singleAlu(int times) {
        int a = (int) (Math.random() * 91 + 10);
        int b = (int) (Math.random() * 91 + 10);

        double temp = 0;
        for (int i = 0; i < times; i++) {
            switch (i % 4) {
                case 0:
                    temp = a + b;
                    break;
                case 1:
                    temp = a - b;
                    break;
                case 2:
                    temp = a * b;
                    break;
                case 3:
                    temp = a / (double) b;
                    break;
            }
        }
        return temp;
    }
}