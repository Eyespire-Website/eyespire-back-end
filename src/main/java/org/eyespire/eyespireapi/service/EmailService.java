package org.eyespire.eyespireapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Xác thực đăng ký - Eyespire");
        message.setText("Mã OTP của bạn là: " + otp + "\nMã sẽ hết hạn sau 5 phút.");

        mailSender.send(message);
    }
    
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu - Eyespire");
        message.setText("Để đặt lại mật khẩu của bạn, vui lòng nhấp vào liên kết sau: \n\n" 
                + resetLink + "\n\n"
                + "Liên kết này sẽ hết hạn sau 24 giờ.\n\n"
                + "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");

        mailSender.send(message);
    }
    
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Đặt lại mật khẩu - Eyespire");
        message.setText("Mã OTP để đặt lại mật khẩu của bạn là: " + otp + 
                "\n\nMã này sẽ hết hạn sau 5 phút." +
                "\n\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.");

        mailSender.send(message);
    }
}