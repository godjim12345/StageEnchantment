package com.gam0zing.stage_enchantment.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicEnchantmentManager {
    // 服务端存储的附魔等级覆盖
    private static final Map<Enchantment, Integer> SERVER_OVERRIDES = new ConcurrentHashMap<>();

    // 动态设置附魔等级上限
    public static void setMaxLevel(Enchantment enchantment, int value) {
        SERVER_OVERRIDES.put(enchantment, value);
    }

    // 加法模式
    public static void addMaxLevel(Enchantment enchantment, int value) {
        SERVER_OVERRIDES.put(enchantment, getDynamicMax(enchantment, enchantment.getMaxLevel()) + value);
    }

    // 获取附魔等级上限
    public static int getDynamicMax(Enchantment enchantment, int defaultVal) {
        return SERVER_OVERRIDES.getOrDefault(enchantment, defaultVal);
    }
}