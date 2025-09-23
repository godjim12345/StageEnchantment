package com.gam0zing.stage_enchantment.events;

import com.gam0zing.stage_enchantment.StageEnchantment;
import com.gam0zing.stage_enchantment.command_pattern.EnchCommand;
import com.gam0zing.stage_enchantment.command_pattern.ICommand;
import com.gam0zing.stage_enchantment.network.NetworkHandler;
import com.gam0zing.stage_enchantment.network.packe.SyncEnchantmentPacket;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.gam0zing.stage_enchantment.StageEnchantment.LOGGER;
import static com.gam0zing.stage_enchantment.StageEnchantment.gson;
import static com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager.*;

/**
 * @author xWode
 * @version 1.0
 */
//服务端功能 处理map数据的转入与转发
@Mod.EventBusSubscriber(modid = StageEnchantment.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class JsonToMapDateHandler {
    private static final String enchJsonName = StageEnchantment.MODID + "-ench.json";
    private static final String commandJsonName = StageEnchantment.MODID + "-commands.json";
    private static File enchJsonFile;
    private static File commandJsonFile;
    //执行一次，在世界创建完成玩家进入前执行，玩家进入多人游戏服务器不会触发
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
        // serverconfig 文件夹下面的json文件
        enchJsonFile = new File(worldDir, "serverconfig/" + enchJsonName);
        commandJsonFile = new File(worldDir, "serverconfig/" + commandJsonName);
        if (commandJsonFile.exists()) {
            try (InputStream in = new FileInputStream(commandJsonFile)){
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                //commandJson格式为：
                //{
                //  "id_1": {
                //    "executed": true,
                //    "effects": {
                //      "minecraft:sharpness": 5,
                //      "minecraft:unbreaking": 3
                //    }
                //  },
                //  "id_2": {
                //    "executed": false,
                //    "effects": {
                //      "minecraft:fortune": 2
                //    }
                //  }
                //}
                JsonObject root = gson.fromJson(json, JsonObject.class);
                root.entrySet().forEach((Map.Entry<String, JsonElement> entry) -> {
                    String commandId = entry.getKey();
                    JsonObject commandObj = entry.getValue().getAsJsonObject();
                    boolean executed = commandObj.get("executed").getAsBoolean();
                    JsonObject effectsObj = commandObj.getAsJsonObject("effects");
                    Map<Enchantment, Integer> effects = new HashMap<>();
                    effectsObj.entrySet().forEach((Map.Entry<String, JsonElement> entry2) -> {
                        String[] split = entry2.getKey().split(":");
                        effects.put(ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.
                                fromNamespaceAndPath(split[0],split[1])),entry2.getValue().getAsInt());

                    });
                    ICommand command = new EnchCommand(effects,executed);
                    COMMANDS.put(commandId,command);
                });
            } catch (IOException ignored) {
            }
        } else {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(commandJsonFile))){
                bw.write(gson.toJson(new JsonObject()));
            } catch (IOException ignored) {
                LOGGER.warn("没有生成指令信息json文件");
            }
        }
        if (enchJsonFile.exists()) {
            try (InputStream in = new FileInputStream(enchJsonFile)){
                String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                //enchJson的内容格式为，保证不会重复：
                //{
                //  "minecraft:sharpness": 5,
                //  "stageenchantment:my_enchant": 3
                //}
                JsonObject root = gson.fromJson(json, JsonObject.class);
                root.entrySet().forEach((Map.Entry<String, JsonElement> entry) -> {
                    String[] split = entry.getKey().split(":");
                    SERVER_OVERRIDES.put(ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.
                            fromNamespaceAndPath(split[0],split[1])),entry.getValue().getAsInt());
                });
            } catch (IOException ignored) {
            }
        } else {
            HashMap<String, Integer> map = new HashMap<>();
            ForgeRegistries.ENCHANTMENTS.forEach((Enchantment enchant) -> {
                ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
                assert id != null;
                int maxLevel = getMaxLevel(enchant);
                map.put(id.toString(), maxLevel);
                SERVER_OVERRIDES.put(enchant, maxLevel);
            });
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(enchJsonFile))){
                bw.write(gson.toJson(map));
            } catch (IOException ignored) {
                LOGGER.warn("没有生成附魔信息json文件");
            }
        }
        if (enchJsonFile.setReadOnly()) {
            LOGGER.info("附魔信息json文件保护已开");
        }
        if (commandJsonFile.setReadOnly()) {
            LOGGER.info("指令信息json文件保护已开");
        }
    }
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (enchJsonFile.setWritable(true)) {
            LOGGER.info("附魔信息json文件保护已关");
        }
        if (commandJsonFile.setWritable(true)) {
            LOGGER.info("指令信息json文件保护已关");
        }
        // 临时 Map，用来序列化指令信息为json
        Map<String, Object> jsonMap = new HashMap<>();
        COMMANDS.forEach((String id,ICommand command) -> {
            EnchCommand cmd = (EnchCommand) command;
            Map<String, Integer> effectsMap = new HashMap<>();
            cmd.effects.forEach((Enchantment enchant, Integer value) -> {
                ResourceLocation enchantId = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
                assert enchantId != null;
                effectsMap.put(enchantId.toString(), value);
            });
            Map<String, Object> cmdMap = new HashMap<>();
            cmdMap.put("executed", cmd.executed);
            cmdMap.put("effects", effectsMap);
            jsonMap.put(id, cmdMap);
        });
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(commandJsonFile))){
            bw.write(gson.toJson(jsonMap));
        } catch (IOException ignored) {
            LOGGER.warn("指令信息json文件写入出现错误");
        }
        //写附魔信息json
        HashMap<String, Integer> map = new HashMap<>();
        ForgeRegistries.ENCHANTMENTS.forEach((Enchantment enchant) -> {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
            assert id != null;
            int maxLevel = getMaxLevel(enchant);
            map.put(id.toString(), maxLevel);
        });
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(enchJsonFile))){
            bw.write(gson.toJson(map));
        } catch (IOException ignored) {
            LOGGER.warn("附魔信息json文件写入出现错误");
        }
    }
    //玩家一进服，就能收到服务端的 SERVER_OVERRIDES 数据
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Map<ResourceLocation, Integer> overrides = new HashMap<>();
            SERVER_OVERRIDES.forEach((enchantment, value) -> overrides.put(ForgeRegistries.ENCHANTMENTS.getKey(enchantment), value));
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    //这里的Packet类，因为在NetworkHandler.register()注册了，有id与之对应，所以不需要传递id，自动补上id
                    new SyncEnchantmentPacket(overrides)
            );
        }
    }
    //向所有在线玩家发送消息
    public static void syncToAllPlayers(Enchantment enchantment,int newMaxLevel) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Map<ResourceLocation, Integer> map = new HashMap<>();
        map.put(ForgeRegistries.ENCHANTMENTS.getKey(enchantment), newMaxLevel);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            NetworkHandler.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncEnchantmentPacket(map)
            );
        }
    }
}
