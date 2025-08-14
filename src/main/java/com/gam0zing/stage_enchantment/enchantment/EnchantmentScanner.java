package com.gam0zing.stage_enchantment.enchantment;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentScanner {

    ///扫描获取已注册的所有附魔，返回包含<附魔.class>的表
    public static List<Class<? extends Enchantment>> get() {
        List<Class<? extends Enchantment>> enchantments = new ArrayList<>();
        for (Enchantment enchantment : ForgeRegistries.ENCHANTMENTS) {
            enchantments.add(enchantment.getClass());
        }
        return enchantments;
    }

}
