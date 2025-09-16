package com.gam0zing.stage_enchantment.utils;

import com.gam0zing.stage_enchantment.StageEnchantment;
import org.jetbrains.annotations.NotNull;

import javax.tools.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static com.gam0zing.stage_enchantment.StageEnchantment.jarPath;

/**
 * @author 向毅灵
 * @version 1.0
 */

public class DynamicCompiler {
    //classpath为在哪里面搜class
    private String classpath;
    //单例
    private static final DynamicCompiler ourInstance = new DynamicCompiler();
    private final URLClassLoader parentClassLoader;
    private boolean createClassFile = false;
    //可临时生成.class文件的地方
    public static String tempClassPath;

    //要先设置
    public void setCreateClassFile(boolean createClassFile) {
        this.createClassFile = createClassFile;
    }

    /**
     * 最先调用
     * @param fileName 除了后缀名的其余部分，也就是包名.类，前面不加src或者src/main/java，如果写://xxx/默认为绝对路径
     * @param code java源代码，最外面只允许有一个""
     * @return 加载到内存里的class
     */
    public Class<?> compileAndLoad(String fileName, String code) throws IOException {
        return jCodeToObj(fileName,code);
    }
    public static DynamicCompiler getInstance() {
        return ourInstance;
    }
    public static void stop() throws IOException {
        ourInstance.parentClassLoader.close();
        deleteDir(Paths.get(tempClassPath));
    }
    public static void deleteDir (Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file); // 删除文件
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir); // 删除空目录
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public byte[] jCodeToClassByte (String fullClassName, String javaCode) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileManager fileManager;
        if (createClassFile) {
            fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        } else {
            // 建立用于保存被编译文件名的对象
            // 每个文件被保存在一个从JavaFileObject继承的类中
            fileManager = new ClassFileManager(compiler.getStandardFileManager(diagnostics, null, null));
        }
        List<JavaFileObject> jFiles = new ArrayList<>();
        jFiles.add(new InMemoryJavaFileObject(fullClassName, javaCode));
        List<String> options = getOptions();
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                options, null, jFiles);
        boolean success = task.call();
        if (success) {
            if (!createClassFile) {
                JavaClassObject jco = ((ClassFileManager)fileManager).getJavaClassObject();
                return jco.getBytes();
            }
        } else {
            //如果想得到具体的编译错误，可以对Diagnostics进行扫描
            StringBuilder error = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                error.append(compilePrint(diagnostic));
            }
            System.out.println(error);
        }
        return null;
    }

    private @NotNull List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add("-encoding");
        options.add("UTF-8");
        if (createClassFile) {
            //设置输出.class文件位置,目录位置
            options.add("-d");
            if (tempClassPath.isEmpty()) {
                throw new RuntimeException("临时生成class路径没有");
            }
            options.add(tempClassPath);
        }
        options.add("-classpath");
        options.add(this.classpath);
        //不让Mixin 的 processor 参与,这样 @Mixin、@Inject 注解都不会被检查，编译器只会当普通注解处理，直接编译
        options.add("-proc:none");
        //是 javac 的一个诊断选项，用来控制编译错误/警告的输出格式,输出详细信息
        options.add("-Xdiags:verbose");
        return options;
    }

    private Class<?> jCodeToObj(String fullClassName, String javaCode) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        ClassFileManager fileManager;
        // 建立用于保存被编译文件名的对象
        // 每个文件被保存在一个从JavaFileObject继承的类中
        fileManager = new ClassFileManager(compiler.getStandardFileManager(diagnostics, null, null));
        List<JavaFileObject> jFiles = new ArrayList<>();
        jFiles.add(new InMemoryJavaFileObject(fullClassName, javaCode));
        List<String> options = new ArrayList<>();
        options.add("-encoding");
        options.add("UTF-8");
        options.add("-classpath");
        options.add(this.classpath);
        //不让Mixin 的 processor 参与,这样 @Mixin、@Inject 注解都不会被检查，编译器只会当普通注解处理，直接编译
        options.add("-proc:none");
        //是 javac 的一个诊断选项，用来控制编译错误/警告的输出格式,输出详细信息
        options.add("-Xdiags:verbose");
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics,
                options, null, jFiles);
        boolean success = task.call();
        if (success) {
            //如果编译成功，用类加载器加载该类
            JavaClassObject jco = fileManager.getJavaClassObject();
            DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(this.parentClassLoader);
            return dynamicClassLoader.loadClass(fullClassName,jco);
        } else {
            //如果想得到具体的编译错误，可以对Diagnostics进行扫描
            StringBuilder error = new StringBuilder();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                error.append(compilePrint(diagnostic));
            }
            System.out.println(error);
        }
        fileManager.close();
        return null;
    }
    private DynamicCompiler() {
        //获取类加载器
        //Java9之后，应用程序和扩展类都不再是 java.net.URLClassLoader 的实例
        //获取包名加类名
        // /E:/保留/javaLearning/StageEnchantment/build/resources/main/%23196!/
        // /E:/保留/我的世界/稳定整合包PCL/早期1.21/.minecraft/versions/1.20.1-Forge_47.4.1/mods/stage_enchantment-1.0.0.jar%23165!/
        URL rootUrl = DynamicCompiler.class.getClassLoader().getResource("");
        this.parentClassLoader = new URLClassLoader(new URL[]{rootUrl});
        //在系统temp目录下面创建一个目录
        try {
            tempClassPath = Files.createTempDirectory
                    ("mc-gam0zing-StageEnchantment").toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.buildClassPath();
    }
    private void buildClassPath() {
        this.classpath = null;
        StringBuilder sb = new StringBuilder();
        for (URL url : this.parentClassLoader.getURLs()) {
            String p = url.getFile();
            sb.append(p).append(File.pathSeparator);
        }
        //添加mod路径 /*表示该目录下所有jar文件
        File file = new File(jarPath.substring(0,jarPath.lastIndexOf("/")));
        String[] list = file.list((dir, name) -> name.endsWith(".jar"));
        assert list != null;
        for (String s : list) {
            sb.append(jarPath, 0, jarPath.lastIndexOf("/")+1).append(s).append(File.pathSeparator);
        }
        sb.append(System.getProperty("java.class.path")).append(File.pathSeparator);
        sb.append(StageEnchantment.minecraftSrgPath).append(File.pathSeparator);
        sb.append(tempClassPath);
        this.classpath = sb.toString();
    }
    //输出编译错误信息
    private String compilePrint(Diagnostic<? extends JavaFileObject> diagnostic) {
        System.out.println("Code:" + diagnostic.getCode());
        System.out.println("Kind:" + diagnostic.getKind());
        System.out.println("Position:" + diagnostic.getPosition());
        System.out.println("Start Position:" + diagnostic.getStartPosition());
        System.out.println("End Position:" + diagnostic.getEndPosition());
        System.out.println("Source:" + diagnostic.getSource());
        System.out.println("Message:" + diagnostic.getMessage(null));
        System.out.println("LineNumber:" + diagnostic.getLineNumber());
        System.out.println("ColumnNumber:" + diagnostic.getColumnNumber());
        return "Code:[" + diagnostic.getCode() + "]\n" +
                "Kind:[" + diagnostic.getKind() + "]\n" +
                "Position:[" + diagnostic.getPosition() + "]\n" +
                "Start Position:[" + diagnostic.getStartPosition() + "]\n" +
                "End Position:[" + diagnostic.getEndPosition() + "]\n" +
                "Source:[" + diagnostic.getSource() + "]\n" +
                "Message:[" + diagnostic.getMessage(null) + "]\n" +
                "LineNumber:[" + diagnostic.getLineNumber() + "]\n" +
                "ColumnNumber:[" + diagnostic.getColumnNumber() + "]\n";
    }
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;
        InMemoryJavaFileObject(String className, String code) {
            //格式："string:///cn/gg/HelloWorld.java"
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }
        //实现getCharContent，使得JavaCompiler可以从content获取java源码
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
    //类文件管理器 将编译好后的class，保存到jClassObject中
    private static class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private JavaClassObject javaClassObject;
        public ClassFileManager(StandardJavaFileManager standardManager) {
            super(standardManager);
        }
        //将编译好后的class，编译好后的Class文件装载进来 保存到jClassObject中
        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            if (javaClassObject == null)
                javaClassObject = new JavaClassObject(className, kind);
            return javaClassObject;
        }

        public JavaClassObject getJavaClassObject() {
            return javaClassObject;
        }
    }
    //将输出流交给JavaCompiler，最后JavaCompiler将编译后的class文件写入输出流中
    private static class JavaClassObject extends SimpleJavaFileObject{
        //定义一个输出流，用于装载JavaCompiler编译后的Class文件
        protected final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        public JavaClassObject(String name, Kind kind) {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
        }
        public byte[] getBytes() {
            return bos.toByteArray();
        }
        //输出流交给JavaCompiler，让它将编译好的Class装载进来
        @Override
        public OutputStream openOutputStream() {
            return bos;
        }
    }
    private static class DynamicClassLoader extends URLClassLoader {
        public DynamicClassLoader(ClassLoader parent) {
            //第一个参数表明没有url路径去解析class文件，这样就不用去特定的路径解析，之后就方便调用loadClass来
            //完成内存中数据流的类加载，第二个参数是表明哪个是自己的父加载器 传入parent为null，也就是父类加载器设置
            // 为BootstrapClassLoader
            super(new URL[0], parent);
        }
        /**
         * 从 URL 搜索路径中查找并加载具有指定名称的类。将根据需要加载和打开引用 JAR 文件的任何 URL，直到找到该类为止。
         * @param className 包名.类名
         * @return 加载的类
         * @throws ClassNotFoundException 没找到类，或者加载器关闭
         */
        @SuppressWarnings({"unused"})
        public Class<?> findClassByClassName(String className) throws ClassNotFoundException {
            //主要职责就是找到.class文件并把.class文件读到内存得到字节码数组，然后调用 defineClass 方法得到 Class 对象。
            // 子类必须实现findClass
            return this.findClass(className);
        }
        public Class<?> loadClass(String fullName, JavaClassObject jco) {
            byte[] classData = jco.getBytes();
            //defineClass 方法的职责是调用 native 方法把 Java 类的字节码解析成一个 Class 对象。
            return this.defineClass(fullName, classData, 0, classData.length);
        }
    }
}
