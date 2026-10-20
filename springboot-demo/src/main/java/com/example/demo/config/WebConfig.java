package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 层配置 —— 目前只做一件事：把上传目录挂成静态资源。
 *
 * 为什么需要它？
 *   用户上传的头像存在项目外的 uploads/ 目录，而 Spring Boot 默认只把
 *   classpath:/static/ 暴露成静态资源。不注册这个映射，数据库里虽然存了
 *   /uploads/avatar/xxx.png，浏览器去请求却是 404。
 *
 * 路径为什么写成 "file:" + 绝对路径？
 *   Spring 的资源定位靠前缀区分来源：classpath: 是打进去的资源，
 *   file: 是磁盘上的文件。上传的图片在后者，所以必须显式加 file: 前缀。
 *   结尾的斜杠也不能少，它表示「这是一个目录，按目录去拼子路径」。
 *
 * ⚠️ 这里【故意不注入 FileStorageService】，而是自己读一遍 app.upload-dir。
 *   原因是踩过一次坑：@WebMvcTest 这类切片测试会加载 @Configuration，
 *   但不会加载 @Service。如果这里注入了 FileStorageService，切片测试就会
 *   因为「找不到这个 Bean」而整个上下文启动失败（TodoControllerTest 15 个用例全红）。
 *   配置类只依赖一个字符串属性，就不会有这种耦合。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;

    public WebConfig(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 转成绝对路径再转 URI。
        // Windows 的路径长这样：D:/xxx/uploads，
        // 里面的反斜杠和盘符直接拼进 URL 会变成怪东西，
        // 所以先转成 URI（file:///D:/xxx/uploads/）再交给 Spring。
        //
        // 注意：这段注释里不能出现「反斜杠 + 字母 u」这种写法。
        // Java 编译器在处理注释之前就会先扫描 Unicode 转义，写成那样会直接编译报错
        // （报「非法的 Unicode 转义」），这是 Java 一个挺有名的坑。
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
