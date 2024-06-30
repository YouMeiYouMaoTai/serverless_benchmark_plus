package test;

import com.google.gson.JsonObject;

public class Hello {
    public static JsonObject main(JsonObject args) {
        String name = "stranger";
        if (args.has("name"))
            name = args.getAsJsonPrimitive("name").getAsString();
        JsonObject response = new JsonObject();
        response.addProperty("greeting", "Hello " + name + "!");
        return response;
    }

    public static void main(String[] args) {
        JsonObject input = new JsonObject();
        input.addProperty("name", "world");
        JsonObject result = main(input);
        System.out.println(result.toString());
    }
}
