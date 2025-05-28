package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.LoginRequest;
import org.eyespire.eyespireapi.dto.LoginResponse;
import org.eyespire.eyespireapi.dto.SignupRequest;
import org.eyespire.eyespireapi.model.OtpCode;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.repository.OtpCodeRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.Random;


@Service
@Transactional
public class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final int OTP_EXPIRE_MINUTES = 5;

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

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private EmailService emailService;

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
        newUser.setRole("patient"); // Mặc định role là USER
        
        // Tạo mật khẩu ngẫu nhiên (người dùng có thể đổi sau)
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        newUser.setPassword(randomPassword);
        
        return userRepository.save(newUser);
    }


    // Step 1: Gửi OTP
    public void signupStep1SendOtp(SignupRequest signupRequest) {
        System.out.println("==> Dữ liệu nhận từ frontend:");
        System.out.println("Name: " + signupRequest.getName());
        System.out.println("Email: " + signupRequest.getEmail());
        System.out.println("Password: " + signupRequest.getPassword());
        logger.info("Bắt đầu signup - gửi OTP cho email: " + signupRequest.getEmail());

        // Kiểm tra email đã có user chưa
        if(userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // Tạo mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu thông tin tạm vào OtpCode
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(signupRequest.getEmail());
        otpCode.setCode(otp);
        otpCode.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES));
        otpCode.setName(signupRequest.getName());
        otpCode.setPassword(signupRequest.getPassword()); // bạn nên hash mật khẩu trước khi lưu, ở đây tạm để vậy

        otpCodeRepository.deleteByEmail(signupRequest.getEmail()); // xóa các otp cũ nếu có
        otpCodeRepository.save(otpCode);

        // Gửi email OTP
        emailService.sendOtpEmail(signupRequest.getEmail(), otp);

        logger.info("Đã gửi OTP cho email: " + signupRequest.getEmail());
    }

    // Step 2: Xác thực OTP & tạo user
    public LoginResponse verifyOtpAndCreateUser(String email, String otp) {
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findByEmailAndCode(email, otp);

        if(otpCodeOpt.isEmpty()) {
            logger.warning("OTP không đúng hoặc không tồn tại");
            return null;
        }

        OtpCode otpCode = otpCodeOpt.get();

        if(otpCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            logger.warning("OTP đã hết hạn");
            otpCodeRepository.deleteByEmail(email);
            return null;
        }

        // Tạo user mới từ thông tin trong otpCode
        String baseUsername = email.substring(0, email.indexOf('@'));
        String finalUsername = baseUsername;
        int counter = 1;
        while (userRepository.findByUsername(finalUsername).isPresent()) {
            finalUsername = baseUsername + counter;
            counter++;
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(finalUsername);
        user.setName(otpCode.getName());
        user.setPassword(otpCode.getPassword()); // Nếu chưa hash, bạn nên hash mật khẩu tại đây hoặc trước khi lưu
        user.setRole("PATIENT");

        user = userRepository.save(user);

        otpCodeRepository.deleteByEmail(email); // xóa otp sau khi dùng

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
    }
}
