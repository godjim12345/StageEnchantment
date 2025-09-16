import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.gam0zing.stage_enchantment.StageEnchantment.jarPath;

/**
 * @author 向毅灵
 * @version 1.0
 */

public class Test {
    public void k () {

    }
    public static void main(String[] args) throws NoSuchMethodException, IOException {
        // 1. 配置路径（你需要按自己的环境修改）
        /*String javaPath = "C:\\Program Files\\Java\\jdk-21\\bin\\javaw.exe";
        String gameDir = "E:\\保留\\我的世界\\稳定整合包PCL\\早期1.21\\.minecraft";
        String version = "1.20.1-Forge_47.4.1";

        // 2. ClassPath：这里随便列几个，真实环境下要完整拼接
        String classpath = String.join(";", new String[]{
                gameDir + "\\libraries\\cpw\\mods\\bootstraplauncher\\1.1.2\\bootstraplauncher-1.1.2.jar",
                gameDir + "\\libraries\\cpw\\mods\\securejarhandler\\1.0.6\\securejarhandler-1.0.6.jar",
                gameDir + "\\libraries\\org\\ow2\\asm\\asm\\9.3\\asm-9.3.jar",
                gameDir + "\\libraries\\cpw\\mods\\modlauncher\\10.0.9\\modlauncher-10.0.9.jar",
                gameDir + "\\libraries\\net\\minecraftforge\\forge\\1.20.1-47.1.0\\forge-1.20.1-47.1.0-universal.jar",
                gameDir + "\\versions\\" + version + "\\" + version + ".jar"
        });

        // 3. 构造 JVM 参数
        List<String> command = new ArrayList<>();
        command.add(javaPath);
        command.add("-Xmx4096m");
        command.add("-Xms1024m");
        command.add("-Djava.library.path=" + gameDir + "\\versions\\" + version + "\\" + version +"-natives");
        command.add("-cp");
        command.add(classpath);

        // 4. 主类（Forge 使用 BootstrapLauncher）
        command.add("cpw.mods.bootstraplauncher.BootstrapLauncher");
        // 5. 游戏参数（可以从登录验证服务获取真实 token，这里演示用假数据）
        command.add("--username");
        command.add("Player123");
        command.add("--version");
        command.add("Forge 1.20.1");
        command.add("--gameDir");
        command.add(gameDir);
        command.add("--assetsDir");
        command.add(gameDir + "\\assets");
        command.add("--assetIndex");
        command.add("5");
        command.add("--uuid");
        command.add("00000000-0000-0000-0000-000000000000");
        command.add("--accessToken");
        command.add("0");
        command.add("--userType");
        command.add("mojang");
        command.add("--versionType");
        command.add("release");

        // 6. 启动进程
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(gameDir)); // 设置工作目录
        pb.inheritIO(); // 继承当前控制台的输出
        Process process = pb.start();

        System.out.println("已启动 Minecraft Forge 1.20.1，PID: " + process.pid());*/
        /*String gameDir = "E:\\保留\\我的世界\\稳定整合包PCL\\早期1.21\\.minecraft";
        String version = "1.20.1-Forge_47.4.1";
        Path versionJsonPath = Paths.get(gameDir, "versions", version, version + ".json");

        // 读取 JSON
        String jsonText = Files.readString(versionJsonPath, StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();

        // 1. 主类
        String mainClass = json.get("mainClass").getAsString();

        // 2. JVM & Game 参数
        List<String> jvmArgs = new ArrayList<>();
        List<String> gameArgs = new ArrayList<>();

        JsonObject arguments = json.getAsJsonObject("arguments");
        JsonArray jvmArray = arguments.getAsJsonArray("jvm");
        JsonArray gameArray = arguments.getAsJsonArray("game");

        for (JsonElement e : jvmArray) {
            if (e.isJsonPrimitive()) {
                jvmArgs.add(e.getAsString());
            }
        }
        for (JsonElement e : gameArray) {
            if (e.isJsonPrimitive()) {
                gameArgs.add(e.getAsString());
            }
        }

        // 3. classpath
        List<String> classpathEntries = new ArrayList<>();
        JsonArray libraries = json.getAsJsonArray("libraries");
        for (JsonElement libE : libraries) {
            JsonObject lib = libE.getAsJsonObject();
            String name = lib.get("name").getAsString(); // 例如 "org.ow2.asm:asm:9.3"
            String[] parts = name.split(":");
            String group = parts[0].replace('.', '/');
            String artifact = parts[1];
            String versionStr = parts[2];
            Path jarPath = Paths.get(gameDir, "libraries", group, artifact, versionStr,
                    artifact + "-" + versionStr + ".jar");
            classpathEntries.add(jarPath.toString());
        }
        // 最后加上版本 jar
        classpathEntries.add(Paths.get(gameDir, "versions", version, version + ".jar").toString());

        // 拼接成命令
        List<String> command = new ArrayList<>();
        command.add("C:\\Program Files\\Java\\jdk-21\\bin\\javaw.exe");
        command.addAll(jvmArgs);
        command.add("-cp");
        command.add(String.join(File.pathSeparator, classpathEntries));
        command.add(mainClass);
        command.addAll(gameArgs);

        // 打印结果
        System.out.println("ddadda-kkkkd大大吗");
        System.out.println("完整启动命令：");
        System.out.println(String.join(" ", command));
        System.out.println("你就");*/
        /*HashMap<String,HashMap<String,String>> srg = new HashMap<>();
        HashMap<String, String> map = new HashMap<>();
        HashMap<String, String> map1 = new HashMap<>();
        map.put("j","o");
        map.put("t","e");
        map1.put("q",";");
        srg.put("k", map);
        srg.put("y", map1);
        JsonObject root = new JsonObject();
        JsonObject mappings = new JsonObject();
        JsonObject date = new JsonObject();
        for (String mixinClassPath : srg.keySet()) {
            JsonObject object = new JsonObject();
            HashMap<String, String> obfuscateMap = srg.get(mixinClassPath);
            for (String obfuscate : obfuscateMap.keySet()) {
                object.addProperty(obfuscate,obfuscateMap.get(obfuscate));
            }
            mappings.add(mixinClassPath,object);
        }
        root.add("mappings",mappings);
        date.add("searge", mappings);
        root.add("date",date);
        System.out.println(new Gson().toJson(root));*/
        System.out.println("k.kl.l".replaceAll("\\.","/"));
    }
}
