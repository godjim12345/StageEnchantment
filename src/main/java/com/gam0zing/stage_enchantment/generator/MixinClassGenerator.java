package com.gam0zing.stage_enchantment.generator;

import com.gam0zing.stage_enchantment.utils.DynamicCompiler;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import static com.gam0zing.stage_enchantment.StageEnchantment.*;
import static com.gam0zing.stage_enchantment.utils.ConfusionParser.getMethodSrgName;
import static com.gam0zing.stage_enchantment.utils.ConfusionParser.isEnchantment;

/**
 * @author 向毅灵
 * @version 1.0
 */
//动态生成mixinClass类，并编译好
public class MixinClassGenerator {
    //编译器实例
    public static final DynamicCompiler instance = DynamicCompiler.getInstance();
    public static final HashMap<String,HashMap<String,String>> enchants = new HashMap<>();
    //类名
    private String className;
    //编译好返回.class的字节码byte数组
    public byte[] generate (String classPathName, String className) {
        this.className = className;
        String src = createSrc(classPathName);
//        writeJavaFile(src,className);
        return instance.jCodeToClassByte("com.gam0zing.stage_enchantment.mixin." + className + "Mixin", src);
    }
    //编译并加载类
    public void generate2 (String classPathName) {
        className = classPathName.substring(classPathName.lastIndexOf(".")+1);
        String src = createSrc(classPathName);
        try {
            Class<?> aClass = instance.compileAndLoad("com.gam0zing.stage_enchantment.mixin." + className + "Mixin", src);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void writeJavaFile (String src,String className) {
        //这里写想要输出的目录绝对地址
        File file = new File("D:\\用户\\Dell\\桌面\\你就\\" + className + "Mixin.java");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(src);
        } catch (IOException ignored) {
        }
    }
    //net.minecraft.world.item.enchantment.ProtectionEnchantment
    public String createSrc (String classPathName) {
        try {
            String srgName = getMethodSrgName(classPathName, "getMaxLevel", "");
            //mixin类的类名
            String mixinClassName = packageName.replace('.','/') + "/mixin/" + className + "Mixin";
            //处理神话
            if (isEnchantment(classPathName)) {
                HashMap<String, String> map = new HashMap<>();
                map.put("getMaxLevel","L" + classPathName + ";"+ srgName +"()I");
                enchants.put(mixinClassName,map);
            }
            //通过反射找到该类
            Class<?> clazz = Class.forName(classPathName);
            //判断是否这个类有没有getMaxLevel方法，没有就要继承父类写mixin
            boolean key = false;
            com.gam0zing.stage_enchantment.domin.Constructor myConstructor = null;
            Class<?> superclass = null;
            boolean isApotheosisClassHook = classPathName.equals(apotheosisClassPath);
            try {
                //只会获取本类的方法，不会获取父类的方法，跳过神话，因为神话那个getMaxLevel有参数
                if (!isApotheosisClassHook) {
                    clazz.getDeclaredMethod(srgName);
                }
            } catch (NoSuchMethodException ignored) {
                key = true;
                superclass = clazz.getSuperclass();
                Constructor<?> constructor = superclass.getDeclaredConstructors()[0];
                myConstructor = new
                        com.gam0zing.stage_enchantment.domin.Constructor(Modifier.toString
                        (constructor.getModifiers()), superclass.getName(),constructor.getParameterTypes());
            }
            if (isApotheosisClassHook) {
                //处理神话
                return "package com.gam0zing.stage_enchantment.mixin;\n" +
                        "\n" +
                        "import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;\n" +
                        "import " + classPathName + ";\n" +
                        "import net.minecraft.world.item.enchantment.Enchantment;\n" +
                        "import org.spongepowered.asm.mixin.Mixin;\n" +
                        "import org.spongepowered.asm.mixin.injection.At;\n" +
                        "import org.spongepowered.asm.mixin.injection.Inject;\n" +
                        "import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;\n" +
                        "\n" +
                        "\n" +
                        "@Mixin(" + className + ".class)\n" +
                        "public class " + className + "Mixin {\n" +
                        "    @Inject(method = \"getMaxLevel\", at = @At(\"RETURN\"), cancellable = true)\n" +
                        "    private static void getMaxLevelMixin(Enchantment e, CallbackInfoReturnable<Integer> cir) {\n" +
                        "        int original = cir.getReturnValue();\n" +
                        "        int newReturn = DynamicEnchantmentManager.getDynamicMax(e, original);\n" +
                        "        cir.setReturnValue(newReturn);\n" +
                        "    }\n" +
                        "}";
            } else if (key) {
                return "package com.gam0zing.stage_enchantment.mixin;\n" +
                        "\n" +
                        "import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;\n" +
                        "import " + classPathName + ";\n" +
                        myConstructor.importString + "\n" +
                        "import org.spongepowered.asm.mixin.Mixin;\n" +
                        "\n" +
                        "\n" +
                        "@Mixin(" + className + ".class)\n" +
                        "public class " + className + "Mixin extends " + superclass.getName() + " {\n" +
                        "    " + myConstructor.modifier + " " + className + "Mixin(" + myConstructor.args + ") {\n" +
                        "        " + myConstructor.superString + "\n" +
                        "    }\n" +
                        "    @Override\n" +
                        "    public int " + srgName + "() {\n" +
                        "        " + className + " enchantment = (" + className + ") (Object) this;\n" +
                        "        return DynamicEnchantmentManager.getDynamicMax(enchantment, super." + srgName + "());\n" +
                        "    }\n" +
                        "}";
            } else {
                return "package com.gam0zing.stage_enchantment.mixin;\n" +
                        "\n" +
                        "import com.gam0zing.stage_enchantment.enchantment.DynamicEnchantmentManager;\n" +
                        "import " + classPathName + ";\n" +
                        "import org.spongepowered.asm.mixin.Mixin;\n" +
                        "import org.spongepowered.asm.mixin.injection.At;\n" +
                        "import org.spongepowered.asm.mixin.injection.Inject;\n" +
                        "import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;\n" +
                        "\n" +
                        "\n" +
                        "@Mixin(" + className + ".class)\n" +
                        "public class " + className + "Mixin {\n" +
                        "    @Inject(method = \"getMaxLevel\", at = @At(\"RETURN\"), cancellable = true)\n" +
                        "    private void getMaxLevelMixin(CallbackInfoReturnable<Integer> cir) {\n" +
                        "        int original = cir.getReturnValue();\n" +
                        "        " + className + " enchantment = (" + className + ") (Object) this;\n" +
                        "        int newReturn = DynamicEnchantmentManager.getDynamicMax(enchantment, original);\n" +
                        "        cir.setReturnValue(newReturn);\n" +
                        "    }\n" +
                        "}";
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    public static void stop (){
        try {
            DynamicCompiler.stop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
