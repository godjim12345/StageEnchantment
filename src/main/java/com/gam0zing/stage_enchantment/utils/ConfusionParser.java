package com.gam0zing.stage_enchantment.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Objects;


/**
 * @author 向毅灵
 * @version 1.0
 */
//混淆解析器，mc混淆逻辑：mojang只提供类名和方法名全部混淆的jar包（混淆后的名字称OfficialName），
// 社区（如forge）统一标识为srg名（类名变回来，方法名属性名变了但还是难懂，如：m_6586_，所以mixin要写refmap.json，写的就是这个srg名），
// 接下来就是为了开发者，在开发环境下再次转mcp名（类名，方法名，属性名全部人能看懂的英文），在开发环境下不会去混淆，
// 所以导师开发环境下加mod会识别不了，报错，因为mod是转srg名了，混淆了，导致很多方法名识别不了。srg名特点：每个版本都对应同一个SRG名
public class ConfusionParser {
    public static final HashMap<String,String> officialClassNames = new HashMap<>();
    public static void obf () {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull
                (JarResources.getResource("server_mappings.txt"))))){
            String line = br.readLine();
            while (line != null) {
                if (!line.startsWith("#")) {
                    String[] split = line.split(" ");
                    //!split[0].isEmpty()确保该行是类的official名，不是类里面的属性和方法名
                    if (!split[0].isEmpty() && split[0].startsWith("net.minecraft.world.item.enchantment")) {
                        officialClassNames.put(split[0],split[2].substring(0,split[2].length()-1));
                    }
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 得到方法夫人srg名
     * @param classPathName 类的全类名（包名.类名）
     * @param methodName 方法的名称
     * @param args 方法的参数列表（java方法描述符表示）
     * @return 方法的srg名
     */
    //todo 还需要改进
    public static String getMethodSrgName(String classPathName,String methodName,String args) {
        if (methodName.equals("getMaxLevel") && args.isEmpty()) {
            return "m_6586_";
        }
        return methodName;
    }
}
