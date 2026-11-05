package com.hanmo.calligraphy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web MVC 配置。
 *
 * <p>把本地上传目录（app.upload.dir）映射为 app.upload.url-prefix 下的静态资源，
 * 使 UploadController 保存的图片可以通过 URL 直接访问。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /**
     * 注册上传目录的静态资源映射。
     *
     * <p>目录会转为绝对路径并补齐结尾的 /，避免相对路径随启动目录变化。</p>
     *
     * @param registry 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = path.toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler(urlPrefix + "/**").addResourceLocations(location);
    }
}
