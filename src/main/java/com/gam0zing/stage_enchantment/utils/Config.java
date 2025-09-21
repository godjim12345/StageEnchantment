package com.gam0zing.stage_enchantment.utils;

import com.gam0zing.stage_enchantment.StageEnchantment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;

import static com.gam0zing.stage_enchantment.StageEnchantment.LOGGER;
import static com.gam0zing.stage_enchantment.StageEnchantment.isServer;
import static com.gam0zing.stage_enchantment.StageEnchantment.version;


/**
 * @author xWode
 * @version 1.0
 */

public class Config {
    //:\...\.minecraft\versions\1.20.1-Forge_47.4.1\config
    public static String configDirPath;
    public static String configFileName = name(StageEnchantment.MOD_NAME) + ".toml";
    public static File configFile;
    //0：报错需要玩家自己关闭窗口并重启
    //1：直接退出，不用弄模组，直接重启
    //2：自动重启（默认值）
    public static int switchKey = 2;

    static {
        configDirPath = FMLPaths.CONFIGDIR.get().toAbsolutePath().toString();
        configFile = new File(configDirPath, configFileName);
        boolean key = false;
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    LOGGER.info("成功创建了Config文件");
                }
            } catch (IOException e) {
                LOGGER.warn("创建不了Config文件");
            }
            key = true;
        }else if (!getValue("version").equals(version)) {
            key = true;
        }
        if (key) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(configFile))){
                if (isServer) {
                    bw.write("#版本信息\r\n");
                    bw.write("version=" + version + "\r\n");
                    bw.write("#0：报错需要服主自己关闭窗口并重启\r\n#1：不报错，但还是服主关闭窗口并重启\r\n#2：自动重启\r\n");
                    bw.write("switchKey=" + switchKey + "\r\n");
                    bw.write("""
                            #默认为根目录下面的run.bat，如果不一样请重新设置\r
                            #这个地址是相对于服务器根目录，假如在mods下面，写:modes/run.bat\r
                            """);
                    bw.write("run=run.bat\r\n");
                    bw.write("""
                            #默认根目录下面的run.bat，里面的最后一行为pause\r
                            #这个为true可以更改为exit，效果为关闭服务器窗口直接关闭，默认为false\r
                            """);
                    bw.write("ifExit=false\r\n");
                } else {
                    bw.write("#版本信息\r\n");
                    bw.write("version=" + version + "\r\n");
                    bw.write("#0：报错需要玩家自己关闭窗口并重启\r\n#1：直接退出，玩家手动重启\r\n#2：自动重启\r\n");
                    bw.write("switchKey=" + switchKey + "\r\n");
                }
            } catch (IOException ignored) {
            }
        }
    }
    public static String name (String configFileName) {
        // 在大写字母前加上 - ，再整体转小写
        return configFileName
                .replaceAll("([a-z])([A-Z])", "$1-$2")
                .toLowerCase();
    }
    public static String getValue (String key) {
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))){
            String line = br.readLine();
            while (line != null) {
                if (!line.startsWith("#")) {
                    String[] split = line.split("=");
                    if (split[0].replaceAll(" ","").equals(key)) {
                        return split[1].replaceAll(" ","");
                    }
                }
                line = br.readLine();
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }
}
