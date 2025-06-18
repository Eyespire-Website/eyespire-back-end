package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.LoginRequest;
import org.eyespire.eyespireapi.dto.LoginResponse;
import org.eyespire.eyespireapi.dto.SignupRequest;
import org.eyespire.eyespireapi.model.OtpCode;
import org.eyespire.eyespireapi.model.PasswordReset;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.repository.OtpCodeRepository;
import org.eyespire.eyespireapi.repository.PasswordResetRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Random;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service class for handling authentication-related tasks.
 */
@Service
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
    private EmailService emailService;

    @Autowired
    private OtpCodeRepository otpCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetRepository passwordResetRepository;

    /**
     * Handles login request.
     *
     * @param loginRequest Login request object containing username and password.
     * @return Login response object containing user information if login is successful, null otherwise.
     */
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

        // Kiểm tra mật khẩu
        if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.info("Đăng nhập thành công cho người dùng: " + user.getUsername());
            LoginResponse response = new LoginResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getGender() != null ? user.getGender().toString() : null,
                    user.getPhone(),
                    user.getAvatarUrl()
            );
            response.setIsGoogleAccount(user.getIsGoogleAccount());
            return response;
        } else {
            logger.warning("Mật khẩu không đúng cho người dùng: " + user.getUsername());
            return null;
        }
    }

    /**
     * Creates Google authorization URL.
     *
     * @return Google authorization URL.
     */
    public String createGoogleAuthorizationUrl() {
        String state = UUID.randomUUID().toString();

        return GOOGLE_AUTH_URL +
                "?client_id=" + googleClientId +
                "&redirect_uri=" + googleRedirectUri +
                "&response_type=code" +
                "&scope=email%20profile" +
                "&state=" + state;
    }

    /**
     * Authenticates with Google using authorization code.
     *
     * @param code Authorization code from Google.
     * @return Login response object containing user information if authentication is successful, null otherwise.
     */
    public LoginResponse authenticateWithGoogle(String code) {
        try {
            // Bước 1: Đổi code lấy access token
            String accessToken = getGoogleAccessToken(code);

            // Kiểm tra nếu không lấy được access token
            if (accessToken == null) {
                logger.severe("Không thể lấy access token từ Google");
                return null;
            }

            // Bước 2: Lấy thông tin người dùng từ Google
            Map<String, Object> userInfo = getGoogleUserInfo(accessToken);

            // Bước 3: Xử lý thông tin người dùng
            String email = (String) userInfo.get("email");

            // Kiểm tra xem email đã tồn tại trong hệ thống chưa
            Optional<User> existingUser = userRepository.findByEmail(email);

            User user;
            if (existingUser.isPresent()) {
                // Người dùng đã tồn tại, cập nhật thông tin từ Google
                user = existingUser.get();
                updateUserFromGoogleInfo(user, userInfo);
                logger.info("Đã cập nhật thông tin người dùng từ Google: " + email);
            } else {
                // Tạo người dùng mới
                user = createUserFromGoogleInfo(userInfo);
                logger.info("Đã tạo người dùng mới từ Google: " + email);
            }

            // Trả về thông tin đăng nhập
            LoginResponse response = new LoginResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getGender() != null ? user.getGender().toString() : null,
                    user.getPhone(),
                    user.getAvatarUrl()
            );
            response.setIsGoogleAccount(true);
            return response;

        } catch (Exception e) {
            logger.severe("Lỗi xác thực Google: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets Google access token using authorization code.
     *
     * @param code Authorization code from Google.
     * @return Google access token if successful, null otherwise.
     */
    private String getGoogleAccessToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("code", code);
            map.add("client_id", googleClientId);
            map.add("client_secret", googleClientSecret);
            map.add("redirect_uri", googleRedirectUri);
            map.add("grant_type", "authorization_code");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        GOOGLE_TOKEN_URL,
                        HttpMethod.POST,
                        request,
                        Map.class
                );

                return (String) response.getBody().get("access_token");
            } catch (HttpClientErrorException e) {
                logger.severe("Lỗi khi lấy access token từ Google: " + e.getMessage());
                // Log chi tiết lỗi để debug
                logger.severe("Response body: " + e.getResponseBodyAsString());
                return null;
            }
        } catch (Exception e) {
            logger.severe("Lỗi không xác định khi lấy access token: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets Google user information using access token.
     *
     * @param accessToken Google access token.
     * @return Map containing user information.
     */
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

    /**
     * Creates a new user from Google user information.
     *
     * @param userInfo Map containing user information.
     * @return Newly created user.
     */
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
        newUser.setRole(UserRole.PATIENT); // Mặc định role là PATIENT

        // Tạo mật khẩu ngẫu nhiên (người dùng có thể đổi sau)
        String randomPassword = UUID.randomUUID().toString().substring(0, 8);
        newUser.setPassword(randomPassword);
        newUser.setIsGoogleAccount(true); // Đánh dấu là tài khoản Google

        return userRepository.save(newUser);
    }

    /**
     * Updates user information from Google user information.
     *
     * @param user    User to update.
     * @param userInfo Map containing user information.
     * @return Updated user.
     */
    private User updateUserFromGoogleInfo(User user, Map<String, Object> userInfo) {
        String name = (String) userInfo.get("name");
        String pictureUrl = (String) userInfo.get("picture");

        // Cập nhật thông tin người dùng
        user.setName(name);
        user.setAvatarUrl(pictureUrl);

        // Lưu người dùng đã cập nhật
        return userRepository.save(user);
    }

    /**
     * Sends OTP for signup.
     *
     * @param signupRequest Signup request object containing user information.
     */
    @Transactional
    public void signupStep1SendOtp(SignupRequest signupRequest) {
        System.out.println("==> Dữ liệu nhận từ frontend:");
        System.out.println("Username: " + signupRequest.getUsername());
        System.out.println("Name: " + signupRequest.getName());
        System.out.println("Email: " + signupRequest.getEmail());
        System.out.println("Password: " + signupRequest.getPassword());
        logger.info("Bắt đầu signup - gửi OTP cho email: " + signupRequest.getEmail());

        // Kiểm tra email đã có user chưa
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // Kiểm tra username đã có user chưa
        if (userRepository.findByUsername(signupRequest.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username đã được sử dụng");
        }

        // Tạo mã OTP 6 số ngẫu nhiên
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu thông tin tạm vào OtpCode
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(signupRequest.getEmail());
        otpCode.setCode(otp);
        otpCode.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRE_MINUTES));
        otpCode.setUsername(signupRequest.getUsername());
        otpCode.setName(signupRequest.getName());
        otpCode.setPassword(passwordEncoder.encode(signupRequest.getPassword()));  // bạn nên hash mật khẩu trước khi lưu, ở đây tạm để vậy

        otpCodeRepository.deleteByEmail(signupRequest.getEmail()); // xóa các otp cũ nếu có
        otpCodeRepository.save(otpCode);

        // Gửi email OTP
        emailService.sendOtpEmail(signupRequest.getEmail(), otp);

        logger.info("Đã gửi OTP cho email: " + signupRequest.getEmail());
    }

    /**
     * Verifies OTP and creates user.
     *
     * @param email Email of the user.
     * @param otp   OTP code.
     * @return Login response object containing user information if successful, null otherwise.
     */
    @Transactional
    public LoginResponse verifyOtpAndCreateUser(String email, String otp) {
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findByEmailAndCode(email, otp);

        if (otpCodeOpt.isEmpty()) {
            logger.warning("OTP không đúng hoặc không tồn tại");
            return null;
        }

        OtpCode otpCode = otpCodeOpt.get();

        if (otpCode.getExpiryTime().isBefore(LocalDateTime.now())) {
            logger.warning("OTP đã hết hạn");
            otpCodeRepository.deleteByEmail(email);
            return null;
        }

        // Tạo user mới từ thông tin trong otpCode
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(otpCode.getUsername());
        newUser.setName(otpCode.getName());
        newUser.setPassword(otpCode.getPassword()); // đã được mã hóa trước đó
        newUser.setRole(UserRole.PATIENT); // mặc định role là PATIENT
        newUser.setIsGoogleAccount(false);

        userRepository.save(newUser);

        otpCodeRepository.deleteByEmail(email); // xóa otp sau khi dùng

        LoginResponse response = new LoginResponse();
        response.setId(newUser.getId());
        response.setUsername(newUser.getUsername());
        response.setName(newUser.getName());
        response.setEmail(newUser.getEmail());
        response.setRole(String.valueOf(newUser.getRole()));
        response.setGender(String.valueOf(newUser.getGender()));
        response.setPhone(newUser.getPhone());
        response.setAvatarUrl(newUser.getAvatarUrl());
        response.setIsGoogleAccount(false);
        return response;
    }

    /**
     * Sends password reset OTP.
     *
     * @param email Email of the user.
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));

        // Kiểm tra xem đây có phải là tài khoản Google không
        if (user.getIsGoogleAccount()) {
            throw new IllegalArgumentException("Đây là tài khoản Google. Vui lòng sử dụng tính năng đăng nhập với Google.");
        }

        // Xóa các OTP cũ nếu có
        passwordResetRepository.deleteByUser(user);

        // Tạo OTP mới (6 chữ số)
        String otp = String.format("%06d", new Random().nextInt(1000000));

        // Lưu OTP vào database
        PasswordReset passwordReset = new PasswordReset();
        passwordReset.setUser(user);
        passwordReset.setOtpCode(otp);
        passwordReset.setExpiresAt(LocalDateTime.now().plusMinutes(5)); // OTP có hiệu lực trong 5 phút

        passwordResetRepository.save(passwordReset);

        // Gửi email chứa OTP
        emailService.sendPasswordResetOtpEmail(user.getEmail(), otp);

        logger.info("Đã gửi email OTP đặt lại mật khẩu cho: " + email);
    }

    /**
     * Resets password using OTP.
     *
     * @param email    Email of the user.
     * @param otp      OTP code.
     * @param newPassword New password.
     * @return True if successful, false otherwise.
     */
    @Transactional
    public boolean resetPassword(String email, String otp, String newPassword) {
        // Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));

        // Kiểm tra xem đây có phải là tài khoản Google không
        if (user.getIsGoogleAccount()) {
            throw new IllegalArgumentException("Đây là tài khoản Google. Vui lòng sử dụng tính năng đăng nhập với Google.");
        }

        // Tìm password reset theo user
        PasswordReset passwordReset = passwordResetRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đặt lại mật khẩu cho email này"));

        // Kiểm tra OTP
        if (!passwordReset.getOtpCode().equals(otp)) {
            throw new IllegalArgumentException("Mã OTP không chính xác");
        }

        // Kiểm tra OTP còn hiệu lực
        if (passwordReset.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetRepository.delete(passwordReset);
            throw new IllegalArgumentException("Mã OTP đã hết hạn");
        }

        // Cập nhật mật khẩu
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Xóa OTP đã sử dụng
        passwordResetRepository.delete(passwordReset);

        logger.info("Đã đặt lại mật khẩu thành công cho user: " + user.getUsername());
        return true;
    }
}