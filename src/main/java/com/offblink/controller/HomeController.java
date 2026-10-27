package com.offblink.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 首页：把 webapp 根目录下的 index.html（接口清单页）原样吐出来。
 *
 * 为什么不交给容器 DefaultServlet：开了它，未匹配的路径会变成 Tomcat 的 HTML 404，
 * 破坏「任何响应都是 Result JSON」的约定（见 SpringMvcConfig 的说明）。
 * 这里只做一件事，页面内容仍然只维护一份（src/main/webapp/index.html），不复制进 Java 字符串。
 */
@RestController
public class HomeController {

    private final ServletContext servletContext;

    /** 页面内容只读一次：文件在运行期不会变，没必要每个请求都做一次 IO */
    private volatile String cachedPage;

    public HomeController(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @GetMapping(value = {"/", "/index.html"}, produces = "text/html;charset=UTF-8")
    public String index() throws IOException {
        String page = cachedPage;
        if (page == null) {
            synchronized (this) {
                if (cachedPage == null) {
                    cachedPage = readIndexPage();
                }
                page = cachedPage;
            }
        }
        return page;
    }

    private String readIndexPage() throws IOException {
        try (InputStream in = servletContext.getResourceAsStream("/index.html")) {
            if (in == null) {
                return "<!DOCTYPE html><meta charset=\"UTF-8\"><h1>JFT-E2 运行中</h1>"
                        + "<p>index.html 未找到，接口清单见 README。</p>";
            }
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
