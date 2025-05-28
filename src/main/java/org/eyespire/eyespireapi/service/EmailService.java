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
}