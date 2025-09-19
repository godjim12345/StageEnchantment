package com.gam0zing.stage_enchantment.utils;

import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.gam0zing.stage_enchantment.StageEnchantment.gson;

/**
 * @author 向毅灵
 * @version 1.0
 */

public class JWTParser {
    private static JsonObject parse;
    /**
     * 解析 Minecraft/Microsoft 的 accessToken (JWT)，输出 xuid、clientId 等字段
     * @param jwt accessToken 字符串
     * @return 返回是否通过解码
     */
    public static boolean parseJwt(String jwt) {
        // JWT 格式: header.payload.signature
        String[] parts = jwt.split("\\.");
        //不是合法的
        if (parts.length < 2) {
            return false;
        }
        // 取 payload 部分（第二段）
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        // 用 Gson 解析 JSON
        parse = gson.fromJson(payload, JsonObject.class);
        return parse != null;
    }
    //launcher_profiles.json的位置 旧版启动器
    public static void load (String filePath) {
        try (InputStream in = new FileInputStream(filePath)){
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = gson.fromJson(json, JsonObject.class);
            JsonObject authDb = root.getAsJsonObject("authenticationDatabase");
            // 通常里面只有一个 key，但 key 是动态的（账号 ID）
            for (String accountId : authDb.keySet()) {
                JsonObject account = authDb.getAsJsonObject(accountId);
                // 取 accessToken
                parseJwt(account.get("accessToken").getAsString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static String getValue (String key) {
        if (parse != null) {
            if (parse.has(key)) {
                return parse.get(key).getAsString();
            }
        }
        return "";
    }
}
