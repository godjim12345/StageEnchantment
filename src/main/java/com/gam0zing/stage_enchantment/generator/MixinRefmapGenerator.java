package com.gam0zing.stage_enchantment.generator;

import com.google.gson.JsonObject;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author xWode
 * @version 1.0
 */

public class MixinRefmapGenerator {
    public static JsonObject refmapJson = new JsonObject();
    //放的是需要被refmap的信息，结构为：类：{方法/属性：srg，方法/属性：srg...}
    private static HashMap<String,HashMap<String,String>> srg = new HashMap<>();
    public static final boolean present = FMLLoader.getNameFunction("srg").isPresent();
    public static class Refmap {
        /**
         * domain 判断是方法还是字段
         * owner 方法或字段的类的全类名
         * name 方法或字段名
         * desc 参数和返回类型 (参数类型列表)返回值类型 目前只有(参数类型列表)或字段类型
         * srg 混淆映射名(参数类型列表)返回值类型 srg混淆映射名字段类型
         * optionsSrg 可能的srg名字
         */
        public INameMappingService.Domain domain;
        public String owner;
        public String name;
        public String desc;
        public String srg;
        public ArrayList<String> optionsSrg = new ArrayList<>();
        public Refmap(INameMappingService.Domain domain, String owner, String name, String desc) {
            this.domain = domain;
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }
        public byte getIndex () {
            return domain == INameMappingService.Domain.METHOD ? (byte) 0 : (byte) 1;
        }
    }
    //todo 目前必须等待MixinClassGenerator全部执行完毕才行，需要改进，并且这个类、ZipReader没用起来，需要改进
    public static void add() {
        srg = MixinClassGenerator.enchants;
        if (!srg.isEmpty()) {
            JsonObject mappings = new JsonObject();
            JsonObject date = new JsonObject();
            for (String mixinClassPath : srg.keySet()) {
                JsonObject object = new JsonObject();
                HashMap<String, String> obfuscateMap = srg.get(mixinClassPath);
                for (String obfuscate : obfuscateMap.keySet()) {
                    object.addProperty(obfuscate,obfuscateMap.get(obfuscate).replace(".","/"));
                }
                mappings.add(mixinClassPath,object);
            }
            refmapJson.add("mappings",mappings);
            date.add("searge", mappings);
            refmapJson.add("date",date);
        }
    }

    /**
     * srg混淆映射名(参数类型列表)返回值类型 srg混淆映射名字段类型
     * @param refmap 需要映射的对象(方法或者字段)
     */
    public static void remapName(Refmap refmap){
        //判断是开发环境还是jar游戏环境，开发环境没有混淆，返回true
        if (present) {
            refmap.srg = refmap.name;
            return;
        }
        Class<?> tClass;
        try {
            tClass = Class.forName(refmap.owner);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        boolean key = true;
        if (refmap.domain == INameMappingService.Domain.METHOD) {
            String k = "";
            for (String s : refmap.optionsSrg) {
                try {
                    k = classToDesc(tClass.getDeclaredMethod(s, descToClass(refmap.desc)).getReturnType());
                } catch (NoSuchMethodException e) {
                    key = false;
                }
                if (key) {
                    refmap.srg = s + k;
                    return;
                }
            }
        } else if (refmap.domain == INameMappingService.Domain.FIELD) {
            for (String s : refmap.optionsSrg) {
                try {
                    tClass.getDeclaredField(s);
                } catch (NoSuchFieldException e) {
                    key = false;
                }
                if (key) {
                    refmap.srg = s + refmap.desc;
                    return;
                }
            }
        }
        refmap.srg = refmap.name;
    }
    //简单参数，八大类型
    private static Class<?>[] descToClass (String desc) {
        String substring = desc.substring(1, desc.lastIndexOf(")"));
        ArrayList<Class<?>> out = new ArrayList<>();
        int length = substring.length();
        for (int i = 0; i < length; i++) {
            if (substring.charAt(i) == 'B') {
                out.add(byte.class);
            } else if (substring.charAt(i) == 'C') {
                out.add(char.class);
            } else if (substring.charAt(i) == 'D') {
                out.add(double.class);
            } else if (substring.charAt(i) == 'F') {
                out.add(float.class);
            } else if (substring.charAt(i) == 'I') {
                out.add(int.class);
            } else if (substring.charAt(i) == 'J') {
                out.add(long.class);
            } else if (substring.charAt(i) == 'S') {
                out.add(short.class);
            } else if (substring.charAt(i) == 'Z') {
                out.add(boolean.class);
            }
        }
        int num = out.size();
        Class<?>[] classes = new Class<?>[num];
        for (int i = 0; i < num; i++) {
            classes[i] = out.get(i);
        }
        return classes;
    }
    //简单八大类型
    private static String classToDesc (Class<?> tClass) {
        if (tClass == byte.class) {
            return "B";
        } else if (tClass == char.class) {
            return "C";
        } else if (tClass == double.class) {
            return "D";
        } else if (tClass == float.class) {
            return "F";
        } else if (tClass == int.class) {
            return "I";
        } else if (tClass == long.class) {
            return "J";
        } else if (tClass == short.class) {
            return "S";
        } else if (tClass == boolean.class) {
            return "Z";
        }
        return "V";
    }
}
