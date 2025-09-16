package com.gam0zing.stage_enchantment.generator;

import com.gam0zing.stage_enchantment.enchantment.EnchantmentInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static com.gam0zing.stage_enchantment.StageEnchantment.enchantments;

/**
 * @author 向毅灵
 * @version 1.0
 */
//mixinConfig.json生成
public class MixinConfigGenerator {
    public static JsonObject mixinConfig;
    public static void add() {
        //添加json数组
        JsonArray array = new JsonArray();
        for (EnchantmentInfo enchantment : enchantments) {
            array.add(enchantment.className+"Mixin");
        }
        mixinConfig.add("mixins", array);
    }
}
