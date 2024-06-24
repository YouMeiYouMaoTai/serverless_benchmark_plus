package test.functions;

import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

@Component
public class Resize {
    public JsonObject call(JsonObject args) {
        String name = "stranger";
        if (args.has("name"))
            name = args.getAsJsonPrimitive("name").getAsString();
        JsonObject response = new JsonObject();
        response.addProperty("greeting", "Hello " + name + "!");
        return response;
    }
}
