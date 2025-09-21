package com.gam0zing.stage_enchantment;

import com.gam0zing.stage_enchantment.enchantment.EnchantmentInfo;
import com.gam0zing.stage_enchantment.generator.MixinClassGenerator;
import com.gam0zing.stage_enchantment.network.NetworkHandler;
import com.gam0zing.stage_enchantment.utils.CreateJar;
import com.gam0zing.stage_enchantment.utils.JWTParser;
import com.gam0zing.stage_enchantment.utils.JarResources;
import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.gam0zing.stage_enchantment.generator.MixinConfigGenerator.mixinConfig;
import static com.gam0zing.stage_enchantment.utils.Config.getValue;
import static com.gam0zing.stage_enchantment.utils.JWTParser.parseJwt;

@Mod(StageEnchantment.MODID)
public class StageEnchantment
{
    public static final String MODID = "stage_enchantment";
    public static final String MOD_NAME = "StageEnchantment";
    public static final Logger LOGGER = LogUtils.getLogger(); //日志，调用它的方法会在logs/latest.log文件里面生成
    public static ArrayList<EnchantmentInfo> enchantments = new ArrayList<>(); //所有附魔信息类，只是类名，不是实例
    public static final String packageName; //com.gam0zing.stage_enchantment 我们的包名，方便后面生成mixin类
    public static final String jarPath; //这个jar包的位置
    public static final String minecraftSrgPath; //社区提供的srg.jar包的位置
    public static final boolean isDevelopmentEnvironment; //判断是否为开发环境
    public static boolean isNeedCreateJar; //判断mixin是否写完全了
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create(); //工具gson
    public static boolean haveApotheosis = false; //判断有无神话
    //神话的getMaxLevel方法所在类
    public static final String apotheosisClassPath = "dev.shadowsoffire.apotheosis.ench.asm.EnchHooks";
    public static final String launcherProfilesPath; //launcher_profiles.json位置 旧版启动器使用 暂时未做功能
    public static boolean isNeedRestart = false;
    public static final boolean isServer;
    public static final String version = "1.0.0";
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
        isServer = FMLEnvironment.dist == Dist.DEDICATED_SERVER;
        if (!isServer) {
            temp = Minecraft.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            minecraftSrgPath = temp.substring(1,temp.lastIndexOf("%"));
        } else {
            minecraftSrgPath = "";
        }
        isDevelopmentEnvironment = !jarPath.endsWith(".jar");
        if (!isDevelopmentEnvironment && !isServer) {
            String minecraft = ".minecraft";
            // :/.../.minecraft
            launcherProfilesPath = minecraftSrgPath.substring
                    (0,minecraftSrgPath.lastIndexOf(minecraft) + minecraft.length()) + "/launcher_profiles.json";
        } else {
            launcherProfilesPath = "";
        }
    }

    public StageEnchantment(FMLJavaModLoadingContext context)
    {
        //Forge 在启动 Mod 时有几个关键阶段：
        //阶段	事件类	触发时机	作用
        //Mod 构造函数	无事件	最早阶段，Mod 实例被创建	只能做简单初始化，不能访问注册表内容
        //Common Setup	FMLCommonSetupEvent	所有注册表都准备好之后，客户端和服务端都会触发	注册网络、逻辑、配方、事件等通用内容
        //Client Setup	FMLClientSetupEvent	只在客户端触发	渲染、键位、GUI 等客户端专属内容
        //Load Complete	FMLLoadCompleteEvent	所有注册、配置加载完毕	可以做最后处理或检查
        //Server Starting	ServerStartingEvent	服务器启动时	初始化服务器逻辑、命令、存档等
        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onLoadComplete);
        modEventBus.addListener(this::setup);
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
    // Common Setup 方法
    private void setup(FMLCommonSetupEvent event) {
        // 注册网络通道和消息
        NetworkHandler.register();
    }
    public void onLoadComplete(FMLLoadCompleteEvent event) {
        // 这里可以安全访问所有模组的内容
        //获取所有已注册附魔，可能有重复的类，但实例一定不会重复，也就是一个类可以创建多个实例附魔对象
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
        //className可能有重复的，但classPathName一定没有
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
        if (isServer) {
            String run = getValue("run");
            File batFile = new File(new File(jarPath.substring(0,jarPath.lastIndexOf("/"))).getParent() + "\\" + run);
            if (!batFile.exists()) {
                LOGGER.warn("bat文件没有，请重新在config里面配置");
                throw new RuntimeException(MOD_NAME + "：bat文件没有，请重新在config里面配置");
            }
            if (Boolean.parseBoolean(getValue("ifExit"))) {
                StringBuilder out = new StringBuilder();
                boolean exitKey = false;
                try (BufferedReader br = new BufferedReader(new FileReader(batFile))){
                    String line = br.readLine();
                    while (line != null) {
                        if (line.equals("exit")) {
                            exitKey = true;
                        }
                        if (!line.equals("pause")) {
                            out.append(line).append("\r\n");
                        }
                        line = br.readLine();
                    }
                }
                if (!exitKey) {
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(batFile))) {
                        out.append("exit");
                        bw.write(out.toString());
                        bw.flush();
                    }
                }
            }
            //带参数数组 + 环境变量 + 工作目录
            Runtime.getRuntime().exec(new String[]{
                    "cmd.exe", "/c", "start", "\"Minecraft Server\"", run
            }, null, batFile.getParentFile());
        } else {
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
}
