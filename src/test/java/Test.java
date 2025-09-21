import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;


/**
 * @author 向毅灵
 * @version 1.0
 */

public class Test {
    public static void main(String[] args) {
        Map<String, Integer> overrides = new HashMap<>();
        overrides.put("minecraft:sharpness", 5);
        overrides.put("stageenchantment:my_enchant", 3);
        overrides.put("stageenchantmeny:my_enchant", 3);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(overrides);
        System.out.println(json);
    }
}
