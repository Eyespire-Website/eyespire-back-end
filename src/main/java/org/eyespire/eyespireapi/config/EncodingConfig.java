package org.eyespire.eyespireapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Configuration class để đảm bảo UTF-8 encoding cho toàn bộ ứng dụng
 * Giải quyết vấn đề hiển thị tiếng Việt trong chẩn đoán và kế hoạch điều trị
 */
@Configuration
public class EncodingConfig {

    /**
     * Bean để cấu hình UTF-8 encoding filter
     * Đảm bảo tất cả request và response đều sử dụng UTF-8
     */
    @Bean
    public CharacterEncodingFilter characterEncodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);
        return filter;
    }
}
