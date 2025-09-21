package com.gam0zing.stage_enchantment.generator;

import com.gam0zing.stage_enchantment.enchantment.EnchantmentInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static com.gam0zing.stage_enchantment.StageEnchantment.*;
import static com.gam0zing.stage_enchantment.StageEnchantment.apotheosisClassPath;

/**
 * @author xWode
 * @version 1.0
 */
//mixinConfig.json生成
public class MixinConfigGenerator {
    public static JsonObject mixinConfig;
    public static void add() {
        //添加json数组
        JsonArray array = new JsonArray();
        if (haveApotheosis) {
            array.add(apotheosisClassPath.substring(apotheosisClassPath.lastIndexOf(".")+1) + "Mixin");
        } else {
            for (EnchantmentInfo enchantment : enchantments) {
                array.add(enchantment.className+"Mixin");
            }
        }
        mixinConfig.add("mixins", array);
    }
}
