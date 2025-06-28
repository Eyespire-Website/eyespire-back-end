package org.eyespire.eyespireapi.config;

import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.eyespire.eyespireapi.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1) // Đảm bảo chạy đầu tiên
public class AdminInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Kiểm tra xem đã có tài khoản admin nào chưa
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);
            
            if (admins.isEmpty()) {
                logger.info("Không tìm thấy tài khoản admin. Đang tạo tài khoản admin mặc định...");
                
                // Tạo tài khoản admin mặc định
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setEmail("admin@eyespire.com");
                adminUser.setPassword(passwordEncoder.encode("admin123")); // Mã hóa mật khẩu trực tiếp
                adminUser.setName("Admin");
                adminUser.setRole(UserRole.ADMIN);
                
                // Lưu tài khoản admin
                User savedAdmin = userRepository.save(adminUser);
                
                logger.info("Đã tạo tài khoản admin mặc định thành công với ID: {}", savedAdmin.getId());
                logger.info("Thông tin đăng nhập admin: username='admin', password='admin123'");
            } else {
                logger.info("Đã tìm thấy {} tài khoản admin trong hệ thống.", admins.size());
                
                // Kiểm tra xem có cần cập nhật mật khẩu admin không
                boolean needUpdate = false;
                User adminUser = admins.get(0);
                
                // Thử kiểm tra mật khẩu (tùy chọn, có thể bỏ nếu không muốn reset mật khẩu)
                if (!passwordEncoder.matches("admin123", adminUser.getPassword())) {
                    logger.info("Cập nhật lại mật khẩu cho tài khoản admin...");
                    adminUser.setPassword(passwordEncoder.encode("admin123"));
                    userRepository.save(adminUser);
                    logger.info("Đã cập nhật mật khẩu admin thành công!");
                    logger.info("Thông tin đăng nhập admin: username='{}', password='admin123'", adminUser.getUsername());
                }
            }
        } catch (Exception e) {
            // Ghi log lỗi nhưng không làm dừng ứng dụng
            logger.error("Có lỗi khi khởi tạo tài khoản admin: {}", e.getMessage(), e);
        }
    }
}
