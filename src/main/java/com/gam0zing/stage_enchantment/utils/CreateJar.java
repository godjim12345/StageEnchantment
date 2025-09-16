package com.gam0zing.stage_enchantment.utils;

import com.gam0zing.stage_enchantment.enchantment.EnchantmentInfo;
import com.gam0zing.stage_enchantment.generator.MixinClassGenerator;
import com.gam0zing.stage_enchantment.generator.MixinConfigGenerator;
import com.gam0zing.stage_enchantment.generator.MixinRefmapGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.jar.*;
import java.time.format.DateTimeFormatter;

import static com.gam0zing.stage_enchantment.StageEnchantment.*;

/**
 * @author 向毅灵
 * @version 1.0
 */
//创建jar
public class CreateJar {
    public static String newJarPath;
    public static void create() throws Exception {
        if (isDevelopmentEnvironment) {
            return;
        }
        File sourceJar = new File(jarPath);
        File outputJar = new File(jarPath.substring(0, jarPath.lastIndexOf("/")),
                jarPath.substring(jarPath.lastIndexOf("/")) + ".disabled");
        newJarPath = outputJar.getPath();
        // 读取源 JAR
        try (JarInputStream jis = new JarInputStream(new FileInputStream(sourceJar));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputJar))) {
            Manifest manifest = jis.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                // 修改 MANIFEST.MF 中的属性
                // 获取当前时间，使用中国标准时间（CST，UTC+8）
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
                // 定义日期时间格式化器 Z偏移量 2025-08-22T15:28:43+0800
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ", Locale.CHINA);
                // 格式化当前时间
                String formattedDate = now.format(formatter);
                attributes.putValue("Implementation-Timestamp", formattedDate);
            }
            // 复制所有原有条目
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                switch (entry.getName()) {
                    case "META-INF/MANIFEST.MF" -> {
                        // 跳过原有的 MANIFEST.MF
                        continue;
                    }
                    case "stage_enchantment.mixins.json" -> {
                        continue;
                    }
                    case "stage_enchantment.refmap.json" -> {
                        continue;
                    }
                }
                jos.putNextEntry(new JarEntry(entry.getName()));
                jis.transferTo(jos);
                jos.closeEntry();
            }
            // 将修改后的 MANIFEST.MF 写入新的 JAR 文件
            if (manifest != null) {
                jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                manifest.write(jos);
                jos.closeEntry();
            }
            // 添加 mixin 类文件
            MixinClassGenerator mixinGenerator = new MixinClassGenerator();
            for (EnchantmentInfo enchantment : enchantments) {
                JarEntry mixinClassEntry = new JarEntry(packageName.replace(".","/")
                        + "/mixin/" + enchantment.className + "Mixin.class");
                jos.putNextEntry(mixinClassEntry);
                jos.write(mixinGenerator.generate(enchantment.classPathName,enchantment.className));
                jos.closeEntry();
            }
            // 添加 mixin config 文件
            JarEntry mixinJsonEntry = new JarEntry("stage_enchantment.mixins.json");
            jos.putNextEntry(mixinJsonEntry);
            MixinConfigGenerator.add();
            jos.write(gson.toJson(MixinConfigGenerator.mixinConfig).getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
            // 添加 refmap json 文件
            JarEntry refmapJsonEntry = new JarEntry("stage_enchantment.refmap.json");
            jos.putNextEntry(refmapJsonEntry);
            MixinRefmapGenerator.add();
            jos.write(gson.toJson(MixinRefmapGenerator.refmapJson).getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
    }
}
