package org.eyespire.eyespireapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Cho phép tất cả các origin (trong môi trường production nên giới hạn cụ thể)
        config.addAllowedOrigin("*");
        
        // Cho phép các header cần thiết
        config.addAllowedHeader("*");
        
        // Cho phép các phương thức HTTP
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        
        // Cho phép gửi cookie (nếu cần)
        config.setAllowCredentials(false);
        
        // Áp dụng cấu hình cho tất cả các endpoint
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
