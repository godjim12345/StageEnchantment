package com.gam0zing.stage_enchantment.enchantment;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.command_pattern.ICommand;
import net.minecraft.world.item.enchantment.Enchantment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gam0zing.stage_enchantment.events.CreativeTabRefreshHandler.flush;
import static com.gam0zing.stage_enchantment.events.JsonToMapDateHandler.syncToAllPlayers;

public class DynamicEnchantmentManager {
    //命令map
    public static final Map<String,ICommand> COMMANDS = new ConcurrentHashMap<>();
    // 服务端存储的附魔等级覆盖 线程安全的Map
    //允许在遍历时 put/remove，不会报错
    public static final Map<Enchantment, Integer> SERVER_OVERRIDES = new ConcurrentHashMap<>();
    // 动态设置附魔等级上限
    public static void setMaxLevel(Enchantment enchantment, int value) {
        SERVER_OVERRIDES.put(enchantment, value);
        if (!StageEnchantment.isServer) {
            flush(enchantment,value);
        } else {
            syncToAllPlayers(enchantment,value);
        }
    }
    // 加法模式
    public static void addMaxLevel(Enchantment enchantment, int value) {
        int newMaxLevel = getDynamicMax(enchantment, getMaxLevel(enchantment)) + value;
        SERVER_OVERRIDES.put(enchantment, newMaxLevel);
        if (!StageEnchantment.isServer) {
            flush(enchantment, newMaxLevel);
        } else {
            syncToAllPlayers(enchantment,newMaxLevel);
        }
    }
    // 获取附魔等级上限
    public static int getDynamicMax(Enchantment enchantment, int defaultVal) {
        return SERVER_OVERRIDES.getOrDefault(enchantment, defaultVal);
    }
    public static int getMaxLevel (Enchantment enchantment) {
        if (!StageEnchantment.haveApotheosis) {
            return enchantment.getMaxLevel();
        } else {
            try {
                Method method = Class.forName(StageEnchantment.apotheosisClassPath).
                        getDeclaredMethod("getMaxLevel", Enchantment.class);
                return (int)method.invoke(null, enchantment);
            } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException |
                     InvocationTargetException e) {
                StageEnchantment.LOGGER.warn("没找到神话附魔类");
                return enchantment.getMaxLevel();
            }
        }
    }
}