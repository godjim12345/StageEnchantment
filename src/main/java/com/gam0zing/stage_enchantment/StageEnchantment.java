package com.gam0zing.stage_enchantment;

import com.gam0zing.stage_enchantment.enchantment.EnchantmentInfo;
import com.gam0zing.stage_enchantment.generator.MixinClassGenerator;
import com.gam0zing.stage_enchantment.utils.CreateJar;
import com.gam0zing.stage_enchantment.utils.JWTParser;
import com.gam0zing.stage_enchantment.utils.JarResources;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.gam0zing.stage_enchantment.generator.MixinConfigGenerator.mixinConfig;
import static com.gam0zing.stage_enchantment.utils.Config.*;
import static com.gam0zing.stage_enchantment.utils.JWTParser.parseJwt;

@Mod(StageEnchantment.MODID)
public class StageEnchantment
{
    private static final String MIXIN_CONFIG_NAME = "stage_enchantment.mixins.json";
    public static final String MODID = "stage_enchantment";
    public static final String MOD_NAME = "StageEnchantment";
    public static final Logger LOGGER = LogUtils.getLogger(); //日志，调用它的方法会在logs/latest.log文件里面生成
    public static ArrayList<EnchantmentInfo> enchantments = new ArrayList<>(); //所有附魔信息类
    public static final String packageName; //com.gam0zing.stage_enchantment 我们的包名，方便后面生成mixin类
    public static final String jarPath; //这个jar包的位置
    public static final String minecraftSrgPath; //社区提供的srg.jar包的位置
    public static final boolean isDevelopmentEnvironment; //判断是否为开发环境
    public static boolean isNeedCreateJar; //判断mixin是否写完全了
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create(); //工具gson
    public static boolean haveApotheosis = false; //判断有无神话
    //神话的getMaxLevel方法所在类
    public static final String apotheosisClassPath = "dev.shadowsoffire.apotheosis.ench.asm.EnchHooks";
    public static final String launcherProfilesPath; //launcher_profiles.json位置
    public static boolean isNeedRestart = false;
    static {
        packageName = StageEnchantment.class.getPackage().getName();
        // /.minecraft/versions/1.20.1-Forge_47.4.1/mods/stage_enchantment-1.0.0.jar#165!/
        // /StageEnchantment/build/resources/main/%23196!/
        //获取当前jar路径
        String temp = StageEnchantment.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        jarPath = temp.substring(1,temp.lastIndexOf("%"));
        // /:/.../.minecraft/libraries/net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-srg.jar%23166!/
        // /.gradle/caches/forge_gradle/minecraft_user_repo/net/minecraftforge
        // /forge/1.20.1-47.4.1_mapped_parchment_2023.09.03-1.20.1
        // /forge-1.20.1-47.4.1_mapped_parchment_2023.09.03-1.20.1.jar%23191!/
        temp = Minecraft.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        minecraftSrgPath = temp.substring(1,temp.lastIndexOf("%"));
        isDevelopmentEnvironment = !jarPath.endsWith(".jar");
        if (!isDevelopmentEnvironment) {
            String minecraft = ".minecraft";
            //:/.../.minecraft
            launcherProfilesPath = minecraftSrgPath.substring
                    (0,minecraftSrgPath.lastIndexOf(minecraft) + minecraft.length()) + "/launcher_profiles.json";
        } else {
            launcherProfilesPath = "";
        }

    }

    public StageEnchantment(FMLJavaModLoadingContext context)
    {
        context.getModEventBus().addListener(this::onLoadComplete);
        //注册一个 JVM 关闭钩子 只要 JVM 结束（包括点叉关闭、崩溃、System.exit()），都会调用这个钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isNeedCreateJar) {
                    Path sourceJar = Paths.get(jarPath);
                    Path outputJar = Paths.get(CreateJar.newJarPath);
                    //如果目标文件已存在，就覆盖
                    Files.move(outputJar,sourceJar, StandardCopyOption.REPLACE_EXISTING);
                    if (isNeedRestart) {
                        restartGame();
                    }
                    LOGGER.info("本身 JAR 已替换: {} {}", sourceJar, outputJar);
                }
            } catch (IOException e) {
                LOGGER.error(e.toString());
            }
        }));
    }
    // 使用SubscribeEvent注解
    @SubscribeEvent
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        // 这里可以安全访问所有模组的内容
        //获取所有已注册附魔，可能有重复的
        for (Enchantment enchant : ForgeRegistries.ENCHANTMENTS) {
            add(enchant);
            /*ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchant);
            System.out.println("附魔ID: " + id + " | 最大等级: " + enchant.getMaxLevel());*/
        }
        isNeedCreateJar = isNeedCreateJar();
        if (isNeedCreateJar) {
            try {
                CreateJar.create();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            MixinClassGenerator.stop();
            JarResources.stop();
            String switchKey = getValue("switchKey");
            int key = 0;
            if (!switchKey.isEmpty()) {
                key = Integer.parseInt(switchKey);
            }
            if (key == 1) {
                System.exit(0);
            } else if (key == 2) {
                isNeedRestart = true;
                System.exit(0);
            } else {
                throw new RuntimeException("更新mixin完毕，请重新启动游戏");
            }
        }
    }
    //为了确保不会加重复的信息，并且检测到神话的附魔，直接跳过
    public static void add(Enchantment enchant) {
        //net.minecraft.world.item.enchantment.ProtectionEnchantment
        //dev.shadowsoffire.apotheosis.ench.replacements.DefenseEnchant
        String classPathName = enchant.getClass().getName();
        for (EnchantmentInfo enchantment : enchantments) {
            if (enchantment.classPathName.equals(classPathName)) {
                return;
            }
        }
        enchantments.add(new EnchantmentInfo(classPathName,classPathName.substring
                (classPathName.lastIndexOf(".")+1)));
        if (classPathName.startsWith("dev.shadowsoffire.apotheosis")) {
            haveApotheosis = true;
        }
    }
    public static boolean isNeedCreateJar () {
        if (isDevelopmentEnvironment) {
            return false;
        }
        // 打开 Jar
        try (InputStream is = JarResources.getResource("stage_enchantment.mixins.json")) {
            // 读取文件内容为字符串
            String json = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
            // 用 Gson 解析为 JsonObject
            mixinConfig = gson.fromJson(json, JsonObject.class);
            //查看json里面有没有mixins这个数组
            if (mixinConfig.has("mixins") && mixinConfig.get("mixins").isJsonArray()) {
                JsonArray mixins = mixinConfig.getAsJsonArray("mixins");
                if (haveApotheosis) {
                    int size = mixins.size();
                    if (mixins.isEmpty()) {
                        return true;
                    }
                    String string = mixins.get(0).getAsString();
                    return !(size == 1 && string.equals(apotheosisClassPath.substring
                            (apotheosisClassPath.lastIndexOf(".")+1) + "Mixin"));
                } else {
                    return !isAllEqual(mixins);
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
    public static boolean isAllEqual (JsonArray jsonArray) {
        //可能有重复的
        ArrayList<String> temp = new ArrayList<>();
        for (EnchantmentInfo enchantment : enchantments) {
            temp.add(enchantment.className + "Mixin");
        }
        ArrayList<String> json = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            json.add(element.getAsString());
        }
        Collections.sort(temp);
        Collections.sort(json);
        return temp.equals(json);
    }
    private void restartGame() throws IOException {
        // Java 可执行路径
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        // JVM 参数
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        // 类路径
        String classpath = System.getProperty("java.class.path");
        // 主类（sun.java.command 包含主类和第一个参数）
        String fullCommand = System.getProperty("sun.java.command");
        String mainClass = fullCommand.split(" ")[0];
        // 其他游戏参数（去掉主类部分）
        String[] parts = fullCommand.split(" ");
        List<String> gameArgs = new ArrayList<>(Arrays.asList(parts).subList(1, parts.length));
        boolean key = parseJwt(gameArgs.get(gameArgs.indexOf("--accessToken") + 1));
        //minecraft游戏id硬编码
        String clientId = "00000000402b5328";
        String xuid = JWTParser.getValue("xuid"); // Forge 1.20.1 不支持 XUID xbox uuid
        // 拼接启动命令
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        cmd.addAll(jvmArgs);
        cmd.add("-cp");
        cmd.add(classpath);
        if (mainClass.endsWith(".jar")) {
            // 如果是 jar，说明启动器用的是 -jar 模式
            cmd.add("-jar");
            cmd.add(mainClass);
        } else {
            // 如果是类，说明正常启动 开发者模式用的
            cmd.add(mainClass);
        }
        gameArgs.add(gameArgs.indexOf("--clientId")+1,clientId);
        if (key) {
            gameArgs.add(gameArgs.indexOf("--xuid")+1,xuid);
        } else {
            gameArgs.remove("--xuid");
        }
        gameArgs.remove("${clientid}");
        gameArgs.remove("${auth_xuid}");
        cmd.addAll(gameArgs);
        // 启动新进程
        new ProcessBuilder(cmd).start();
    }
}
