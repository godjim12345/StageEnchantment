package com.gam0zing.stage_enchantment.enchantment;

/**
 * @author 向毅灵
 * @version 1.0
 */
//附魔类的信息
public class EnchantmentInfo {
    //类的全类名（包名加类名）
    public String classPathName;
    //类的类名
    public String className;
    //该附魔的原始附魔上限值（getMaxLevel）
    public int original;
    public EnchantmentInfo(String classPathName, String className, int original) {
        this.classPathName = classPathName;
        this.className = className;
        this.original = original;
    }

    @Override
    public String toString() {
        return "EnchantmentInfo{" +
                "classPathName='" + classPathName + '\'' +
                ", className='" + className + '\'' +
                ", original=" + original +
                '}';
    }
}
