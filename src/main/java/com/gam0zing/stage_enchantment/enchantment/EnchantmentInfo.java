package com.gam0zing.stage_enchantment.enchantment;

/**
 * @author xWode
 * @version 1.0
 */
//附魔类的信息
public class EnchantmentInfo {
    //类的全类名（包名加类名）
    public String classPathName;
    //类的类名
    public String className;
    public EnchantmentInfo(String classPathName, String className) {
        this.classPathName = classPathName;
        this.className = className;
    }

    @Override
    public String toString() {
        return "EnchantmentInfo{" +
                "classPathName='" + classPathName + '\'' +
                ", className='" + className + '\'' +
                '}';
    }
}
