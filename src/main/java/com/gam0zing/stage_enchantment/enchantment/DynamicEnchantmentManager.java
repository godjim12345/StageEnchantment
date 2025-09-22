package com.gam0zing.stage_enchantment.enchantment;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.command_pattern.EnchCommand;
import com.gam0zing.stage_enchantment.command_pattern.ICommand;
import net.minecraft.world.item.enchantment.Enchantment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.gam0zing.stage_enchantment.events.CreativeTabRefreshHandler.flush;
import static com.gam0zing.stage_enchantment.events.JsonToMapDateHandler.syncToAllPlayers;

public class DynamicEnchantmentManager {

    public static final Map<String, ICommand> COMMANDS = new ConcurrentHashMap<>();                 //命令Map
    public static final Map<Enchantment, Integer> SERVER_OVERRIDES = new ConcurrentHashMap<>();     //附魔Map

    //#region 附魔Map相关
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
    //#endregion

    //#region 命令Map相关

    /// 创建Command：
    /// @return 创建成功为 true，失败为 false
    public static boolean createCommand(String commandID) {

        if (COMMANDS.containsKey(commandID)) return false;

        COMMANDS.put(commandID, new EnchCommand());

        return true;
    }

    /// 销毁Command：
    /// @return 销毁成功为 true，失败为 false
    public static boolean destroyCommand(String commandID) {

        if (!COMMANDS.containsKey(commandID)) return false;

        //此时需要unexecute，如果该Command已经生效了，需要在销毁前撤销
        COMMANDS.get(commandID).unexecute();
        COMMANDS.remove(commandID);

        return true;
    }

    /// 设置效果：
    /// @return Command存在：添加新效果返回 1，覆盖原有效果返回 0。Command不存在：返回 -1
    public static int setEffect(String commandID, Enchantment ench, int value) {

        if (!COMMANDS.containsKey(commandID)) return -1;

        var retValue = COMMANDS.get(commandID).setEffect(ench, value);

        return retValue ? 1 : 0;
    }

    /// 移除效果：
    /// @return 移除成功返回 true，移除失败返回 false
    public static boolean removeEffect(String commandID, Enchantment ench) {

        if (!COMMANDS.containsKey(commandID)) return false;

        return COMMANDS.get(commandID).removeEffect(ench);
    }

    /// 执行Command
    /// @return Command存在：执行成功返回 1，执行失败返回 0。Command不存在：返回 -1
    public static int execute(String commandID) {

        if (!COMMANDS.containsKey(commandID)) return -1;

        var retValue = COMMANDS.get(commandID).execute();

        return retValue ? 1 : 0;
    }

    /// 撤销Command
    /// @return Command存在：撤销成功返回 1，撤销失败返回 0。Command不存在：返回 -1
    public static int unexecute(String commandID) {

        if (!COMMANDS.containsKey(commandID)) return -1;

        var retValue = COMMANDS.get(commandID).unexecute();

        return retValue ? 1 : 0;
    }

    //#endregion
}