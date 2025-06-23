package org.eyespire.eyespireapi.dto;

/**
 * DTO cho request gửi email thông tin đăng nhập
 */
public class LoginCredentialsEmailRequest {
    private String recipientEmail;
    private String recipientName;
    private String username;
    private String password;
    private String subject;

    public LoginCredentialsEmailRequest() {
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
