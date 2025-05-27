package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.LoginRequest;
import org.eyespire.eyespireapi.dto.LoginResponse;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${google.client.secret}")
    private String googleClientSecret;

    @Value("${google.redirect.uri}")
    private String googleRedirectUri;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    public LoginResponse login(LoginRequest loginRequest) {
        logger.info("Đang xử lý đăng nhập cho username/email: " + loginRequest.getUsername());
        
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseGet(() -> userRepository.findByEmail(loginRequest.getUsername())
                        .orElse(null));
        
        // Log kết quả tìm kiếm user
        if (user == null) {
            logger.warning("Không tìm thấy người dùng với username/email: " + loginRequest.getUsername());
            return null;
        }
        
        logger.info("Tìm thấy người dùng: " + user.getUsername() + ", đang kiểm tra mật khẩu");
        
        // Kiểm tra nếu user tồn tại và mật khẩu đúng
        if (user.getPassword().equals(loginRequest.getPassword())) {
            logger.info("Đăng nhập thành công cho người dùng: " + user.getUsername());
            return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getGender(),
                user.getPhone(),
                user.getAvatarUrl()
            );
        } else {
            logger.warning("Mật khẩu không đúng cho người dùng: " + user.getUsername());
            return null;
        }
    }
    
    public String createGoogleAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        
        return GOOGLE_AUTH_URL + 
                "?client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=email%20profile" +
                "&state=" + state;
    }
    
    public LoginResponse authenticateWithGoogle(String code) {
        try {
            // Bước 1: Đổi code lấy access token
            String accessToken = getGoogleAccessToken(code);
            
            // Bước 2: Lấy thông tin người dùng từ Google
            Map<String, Object> userInfo = getGoogleUserInfo(accessToken);
            
            // Bước 3: Xử lý thông tin người dùng
            String email = (String) userInfo.get("email");
            
            // Kiểm tra xem email đã tồn tại trong hệ thống chưa
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            User user;
            if (existingUser.isPresent()) {
                // Người dùng đã tồn tại, cập nhật thông tin nếu cần
                user = existingUser.get();
                logger.info("Người dùng đã tồn tại trong hệ thống: " + email);
            } else {
                // Tạo người dùng mới
                user = createUserFromGoogleInfo(userInfo);
                logger.info("Đã tạo người dùng mới từ Google: " + email);
            }
            
            // Trả về thông tin đăng nhập
            return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getGender(),
                user.getPhone(),
                user.getAvatarUrl()
            );
            
        } catch (Exception e) {
            logger.severe("Lỗi xác thực Google: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String getGoogleAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", code);
        map.add("client_id", googleClientId);
        map.add("client_secret", googleClientSecret);
        map.add("redirect_uri", googleRedirectUri);
        map.add("grant_type", "authorization_code");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_TOKEN_URL,
                HttpMethod.POST,
                request,
                Map.class
        );
        
        return (String) response.getBody().get("access_token");
    }
    
    private Map<String, Object> getGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_USER_INFO_URL,
                HttpMethod.GET,
                entity,
                Map.class
        );
        
        return response.getBody();
    }
    
    private User createUserFromGoogleInfo(Map<String, Object> userInfo) {
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.get("name");
        String pictureUrl = (String) userInfo.get("picture");
        
        // Tạo username từ email (phần trước @)
        String username = email.substring(0, email.indexOf('@'));
        
        // Kiểm tra xem username đã tồn tại chưa
        int counter = 1;
        String finalUsername = username;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = username + counter;
            counter++;
        }
        
        // Tạo user mới
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(finalUsername);
        newUser.setName(name);
        newUser.setAvatarUrl(pictureUrl);
        newUser.setRole("USER"); // Mặc định role là USER
        
        // Tạo mật khẩu ngẫu nhiên (người dùng có thể đổi sau)
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        newUser.setPassword(randomPassword);
        
        return userRepository.save(newUser);
    }
}
