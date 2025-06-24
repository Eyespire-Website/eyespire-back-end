package org.eyespire.eyespireapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class TimeConverter implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToLocalTimeConverter());
    }

    /**
     * Chuyển đổi chuỗi thời gian thành LocalTime
     * Hỗ trợ nhiều định dạng thời gian khác nhau
     */
    private static class StringToLocalTimeConverter implements Converter<String, LocalTime> {
        @Override
        public LocalTime convert(String source) {
            if (source == null || source.isEmpty()) {
                return null;
            }

            // Thử các định dạng thời gian khác nhau
            try {
                // Định dạng HH:mm:ss
                if (source.length() == 8) {
                    return LocalTime.parse(source, DateTimeFormatter.ofPattern("HH:mm:ss"));
                }
                // Định dạng HH:mm
                else if (source.length() == 5) {
                    return LocalTime.parse(source, DateTimeFormatter.ofPattern("HH:mm"));
                }
                // Thử với định dạng ISO chuẩn
                else {
                    return LocalTime.parse(source);
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Định dạng thời gian không hợp lệ: " + source, e);
            }
        }
    }
}
