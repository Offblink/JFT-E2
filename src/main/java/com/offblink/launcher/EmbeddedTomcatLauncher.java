package com.offblink.launcher;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

/**
 * 本地开发用的内嵌 Tomcat 9 启动器（免安装独立 Tomcat）。
 *
 * 为什么不是 tomcat7-maven-plugin：
 *   该插件最后版本是 2013 年的 2.2，编译时按 Tomcat 7 API 写死了 {@code Tomcat.setDefaultRealm(Realm)}，
 *   而 Tomcat 8 起这个方法已被删除 —— 把 embed-core 顶到 9.x 必然 NoSuchMethodError（实测）；
 *   用它自带的 Tomcat 7 又只有 Servlet 3.0，低于 Spring 5.3 要求的 3.1。
 *   所以直接用 embed-core 9 起容器，类路径来自本工程 pom（provided 作用域，不进 war）。
 *
 * 部署目录用「展开的 war」（target/JFT-E2，由 mvn package / war:war 生成）：
 *   Servlet 3.0 的 SpringServletContainerInitializer 只从 WEB-INF/lib 与 WEB-INF/classes
 *   里读 META-INF/services —— 直接指向 src/main/webapp 时依赖都在 JVM 类路径上、不在 WEB-INF/lib 里，
 *   SCI 扫不到 → DispatcherServlet 根本不注册，所有 /emps 都是 Tomcat 的 404（实测踩过）。
 *   展开的 war 与真实部署结构一致，行为才一致。
 *
 * 启动：mvn package exec:java    （默认 8082，改端口：-Dserver.port=8090）
 */
public final class EmbeddedTomcatLauncher {

    private EmbeddedTomcatLauncher() {
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("server.port", "8082"));

        String finalName = System.getProperty("app.finalName", "JFT-E2");
        File warDir = new File("target/" + finalName);
        File sourceDir = new File("src/main/webapp");
        File docBase = warDir.isDirectory() ? warDir : sourceDir;

        if (docBase == sourceDir) {
            System.out.println();
            System.out.println("!! 未找到展开的 war（target/" + finalName + "），回退到 src/main/webapp。");
            System.out.println("   Spring 的 ServletContainerInitializer 在源码目录下扫不到依赖，");
            System.out.println("   接口会全部 404 —— 请用：mvn package exec:java");
            System.out.println();
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        // 工作目录落在 target 下：JSP 编译产物、临时文件都不污染工程根目录
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        // 触发连接器创建（Tomcat 9 里不调用它，start() 时就没有 Connector）
        tomcat.getConnector();

        Context context = tomcat.addWebapp("", docBase.getAbsolutePath());
        // 关键：webapp 类加载器的父加载器必须显式指向「加载了本启动器的类加载器」。
        // 缺省值是系统类加载器 —— 在 mvn exec:java（进程内执行）下那是 Maven 自己的加载器，
        // 既没有 Servlet API 也没有 Spring，Servlet 3.0 的 SCI 扫描会在
        // javax.servlet.ServletContainerInitializer 上直接 ClassNotFoundException（实测踩过）。
        context.setParentClassLoader(EmbeddedTomcatLauncher.class.getClassLoader());

        tomcat.start();
        System.out.println();
        System.out.println("  JFT-E2 已启动 → http://localhost:" + port + "/");
        System.out.println("  部署目录：" + docBase.getAbsolutePath());
        System.out.println("  登录示例：POST /login {\"username\":\"admin\",\"password\":\"123456\"}");
        System.out.println();
        tomcat.getServer().await();
    }
}
