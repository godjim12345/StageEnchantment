package com.gam0zing.stage_enchantment.domin;

/**
 * @author 向毅灵
 * @version 1.0
 */
//构造器的写法
public class Constructor {
    //构造器修饰符
    public String modifier;
    //需要import的类
    public String importString;
    //构造器参数
    public String args;
    //写super
    public String superString;

    //public transient
    //[class net.minecraft.world.item.enchantment.Enchantment$Rarity,
    //class net.minecraft.world.item.enchantment.ProtectionEnchantment$Type,
    //class [Lnet.minecraft.world.entity.EquipmentSlot;]
    public Constructor(String modifier,String superClassPath, Class<?>[] parameterTypes) {
        this.modifier = modifier.split(" ")[0];
        StringBuilder importSb = new StringBuilder();
        StringBuilder argsSb = new StringBuilder();
        StringBuilder superSb = new StringBuilder();
        superSb.append("super(");
        int k = 0;
        for (Class<?> parameterType : parameterTypes) {
            //net.minecraft.world.item.enchantment.Enchantment$Rarity
            //[Lnet.minecraft.world.entity.EquipmentSlot;
            String name = parameterType.getName();
            String argsTemp = "";
            //说明是数组
            if (name.startsWith("[L")) {
                name = name.substring(2,name.length()-1);
                argsTemp = "[]";
            }
            //分开内部类
            String[] split = name.split("\\$");
            if (split[0].contains(".")) {
                importSb.append("import ").append(split[0]).append(";\n");
            }
            //纯类名
            String temp = split[0].substring(split[0].lastIndexOf(".")+1);
            String argsName;
            if (split.length > 1) {
                temp = temp + "." + split[1] + argsTemp + " a" + split[1] + k;
                argsName = "a" + split[1] + k;
            } else {
                argsName = "a" + temp + k;
                temp = temp + argsTemp + " a" + temp + k;
            }
            //形参编号
            k++;
            argsSb.append(temp).append(", ");
            superSb.append(argsName).append(",");
        }
        int i = importSb.indexOf(superClassPath);
        if (i == -1) {
            importSb.append("import ").append(superClassPath).append(";");
        } else {
            importSb.delete(importSb.length()-1,importSb.length());
        }
        argsSb.delete(argsSb.length()-2,argsSb.length());
        superSb.delete(superSb.length()-1,superSb.length());
        superSb.append(");");
        importString = importSb.toString();
        args = argsSb.toString();
        superString = superSb.toString();
    }
}
