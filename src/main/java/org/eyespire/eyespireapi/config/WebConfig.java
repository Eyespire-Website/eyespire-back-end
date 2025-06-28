package org.eyespire.eyespireapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:${user.home}/eyespire/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Đăng ký handler để phục vụ file từ thư mục uploads
        Path uploadPath = Paths.get(uploadDir);
        String uploadAbsolutePath = uploadPath.toFile().getAbsolutePath();
        
        // Đảm bảo đường dẫn kết thúc bằng dấu /
        if (!uploadAbsolutePath.endsWith("/") && !uploadAbsolutePath.endsWith("\\")) {
            uploadAbsolutePath += "/";
        }
        
        System.out.println("Serving static files from: " + uploadAbsolutePath);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadAbsolutePath);
    }
}
