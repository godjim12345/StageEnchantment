package com.gam0zing.stage_enchantment.utils;

import com.gam0zing.stage_enchantment.StageEnchantment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;

import static com.gam0zing.stage_enchantment.StageEnchantment.LOGGER;


/**
 * @author 向毅灵
 * @version 1.0
 */

public class Config {
    //:\...\.minecraft\versions\1.20.1-Forge_47.4.1\config
    public static String configDirPath;
    public static String configFileName = StageEnchantment.MOD_NAME + ".toml";
    public static File configFile;
    //0：报错需要玩家自己关闭窗口并重启
    //1：直接退出，不用弄模组，直接重启
    //2：自动重启
    public static int switchKey = 0;

    static {
        configDirPath = FMLPaths.CONFIGDIR.get().toAbsolutePath().toString();
        configFile = new File(configDirPath, configFileName);
        if (!configFile.exists()) {
            try {
                if (configFile.createNewFile()) {
                    LOGGER.info("成功创建了Config文件");
                }
            } catch (IOException e) {
                LOGGER.warn("创建不了Config文件");
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(configFile))){
                bw.write("#0：报错需要玩家自己关闭窗口并重启\r\n#1：直接退出，不用弄模组，直接重启\r\n#2：自动重启\r\n");
                bw.write("switchKey=" + switchKey + "\r\n");
            } catch (IOException ignored) {
            }
        }
    }
    public static String getValue (String key) {
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))){
            String line = br.readLine();
            while (line != null) {
                if (!line.startsWith("#") && line.startsWith(key)) {
                    return line.split("=")[1].replaceAll(" ","");
                }
                line = br.readLine();
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }
}
