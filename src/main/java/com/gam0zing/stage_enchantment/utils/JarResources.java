package com.gam0zing.stage_enchantment.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static com.gam0zing.stage_enchantment.StageEnchantment.jarPath;

/**
 * @author 向毅灵
 * @version 1.0
 */
//自身jar包资源获取
public class JarResources {
    private static final JarFile jar;
    static {
        try {
            jar = new JarFile(new File(jarPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static InputStream getResource (String path) {
        // 打开 Jar
        try {
            ZipEntry entry = jar.getEntry(path);
            if (entry != null) {
                return jar.getInputStream(entry);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
    public static void stop () {
        try {
            jar.close();
        } catch (IOException ignored) {
        }
    }
}
