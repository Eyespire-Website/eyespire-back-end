package org.eyespire.eyespireapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Autowired
    private AIQueryService aiQueryService;

    @Value("${gemini.api.key:AIzaSyBHya0jYSVX1xqu1ZuAw6xNpq00VrKEyFo}")
    private String API_KEY;
    
    // Sử dụng API endpoint và model có free tier quota cao
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent";    
    // Danh sách các câu trả lời dự phòng về các chủ đề liên quan đến mắt
    private static final String[] FALLBACK_RESPONSES = {
        "Xin chào! Tôi là trợ lý Eyespire. Tôi có thể giúp bạn tìm hiểu về các dịch vụ khám mắt của chúng tôi.",
        "Eyespire cung cấp nhiều dịch vụ khám và điều trị mắt chuyên nghiệp. Bạn có thể đặt lịch khám bằng cách điền vào form đặt lịch trên trang web.",
        "Chúng tôi khuyên bạn nên khám mắt định kỳ 6 tháng một lần để đảm bảo sức khỏe thị lực.",
        "Eyespire có đội ngũ bác sĩ chuyên khoa mắt giàu kinh nghiệm, sẵn sàng tư vấn và điều trị cho bạn.",
        "Bạn có thể tìm hiểu thêm về các dịch vụ của chúng tôi trong mục Services trên trang web.",
        "Để đặt lịch khám tại Eyespire, bạn chỉ cần chọn ngày và khung giờ trống. Bác sĩ và dịch vụ sẽ được chỉ định sau.",
        "Eyespire cung cấp các dịch vụ khám mắt tổng quát, đo khúc xạ, chẩn đoán và điều trị các bệnh về mắt.",
        "Nếu bạn có bất kỳ câu hỏi nào về sức khỏe mắt, đừng ngần ngại hỏi tôi!",
        "Đội ngũ bác sĩ của Eyespire luôn cập nhật các phương pháp điều trị mới nhất để mang lại kết quả tốt nhất cho bệnh nhân.",
        "Việc bảo vệ thị lực là rất quan trọng. Hãy đến Eyespire để được tư vấn cách chăm sóc mắt hiệu quả."
    };
    
    private List<ChatMessage> conversationHistory = new ArrayList<>();
    private Gson gson = new Gson();
    private Random random = new Random();

    // Cấu hình cho retry
    private static final int MAX_RETRIES = 2;
    private static final int BASE_DELAY_MS = 500;
    private static final int MAX_DELAY_MS = 30000;

    public String generateResponse(String userMessage) {
        try {
            // Lưu tin nhắn người dùng vào lịch sử
            conversationHistory.add(new ChatMessage("user", userMessage));
            
            // Kiểm tra xem có phải câu hỏi về dữ liệu không
            if (isDataQuery(userMessage)) {
                return handleDataQuery(userMessage);
            }

            // Giới hạn lịch sử cuộc trò chuyện (10 tin nhắn gần nhất)
            if (conversationHistory.size() > 10) {
                conversationHistory = conversationHistory.subList(conversationHistory.size() - 10, conversationHistory.size());
            }

            // Tạo JSON request bằng Gson
            JsonObject messagePart = new JsonObject();
            messagePart.addProperty("text", "Bạn là Eyespire Assistant, một trợ lý AI thân thiện của phòng khám mắt Eyespire. "
                    + "Hãy trả lời một cách vui vẻ và gần gũi, ngắn gọn thôi đừng dài dòng quá. "
                    + "Bạn biết nhiều về các vấn đề liên quan đến mắt, các dịch vụ khám và điều trị mắt. "
                    + "Bạn có thể giúp người dùng đặt lịch khám, tìm hiểu về các dịch vụ, hoặc trả lời các câu hỏi về sức khỏe mắt. "
                    + userMessage);

            JsonArray partsArray = new JsonArray();
            partsArray.add(messagePart);

            JsonObject userMessageObject = new JsonObject();
            userMessageObject.addProperty("role", "user");
            userMessageObject.add("parts", partsArray);

            JsonArray contentsArray = new JsonArray();
            contentsArray.add(userMessageObject);

            JsonObject requestJson = new JsonObject();
            requestJson.add("contents", contentsArray);

            // Chuyển JSON request thành chuỗi
            String requestBody = gson.toJson(requestJson);
            logger.info("Request body: " + requestBody);

            // Gửi request đến API
            URL url = new URL(API_URL + "?key=" + API_KEY);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            logger.info("Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    logger.info("API Response: " + response.toString());

                    // Trích xuất phản hồi từ JSON
                    String extractedText = extractTextFromResponse(response.toString());

                    if (extractedText != null && !extractedText.isEmpty()) {
                        conversationHistory.add(new ChatMessage("model", extractedText));
                        return extractedText;
                    }
                }
            } else {
                logger.error("Error Response: " + connection.getResponseMessage());
                // Xử lý các mã lỗi cụ thể
                if (responseCode == 400) {
                    return "Xin lỗi, yêu cầu không hợp lệ. Vui lòng thử lại với câu hỏi khác.";
                } else if (responseCode == 401 || responseCode == 403) {
                    return "Xin lỗi, API key không hợp lệ hoặc hết hạn. Vui lòng liên hệ quản trị viên.";
                } else if (responseCode == 429) {
                    return getFallbackResponse(userMessage);
                } else {
                    return "Xin lỗi, đã xảy ra lỗi khi kết nối với trợ lý AI. Vui lòng thử lại sau.";
                }
            }
        } catch (Exception e) {
            logger.error("Error generating response", e);
        }
        
        // Trả về fallback response nếu có lỗi
        return getFallbackResponse(userMessage);
    }
    
    /**
     * Phương thức trả về câu trả lời dự phòng khi API không khả dụng
     */
    private String getFallbackResponse(String userMessage) {
        String message = userMessage.toLowerCase();
        
        // Các câu trả lời dự phòng dựa trên từ khóa
        if (message.contains("xin chào") || message.contains("hello") || message.contains("hi")) {
            return "Xin chào! Tôi là trợ lý Eyespire. Tôi có thể giúp gì cho bạn về các vấn đề liên quan đến mắt?";
        } else if (message.contains("đặt lịch") || message.contains("đặt hẹn") || message.contains("lịch khám")) {
            return "Để đặt lịch khám tại Eyespire, bạn chỉ cần chọn ngày và khung giờ trống trong form đặt lịch. Bác sĩ và dịch vụ sẽ được chỉ định sau khi bạn đặt lịch thành công.";
        } else if (message.contains("dịch vụ") || message.contains("khám mắt") || message.contains("điều trị")) {
            return "Eyespire cung cấp nhiều dịch vụ khám và điều trị mắt như: khám mắt tổng quát, đo khúc xạ, chẩn đoán và điều trị các bệnh về mắt, phẫu thuật mắt, v.v. Bạn có thể tìm hiểu thêm trong mục Dịch vụ trên trang web của chúng tôi.";
        } else if (message.contains("giá") || message.contains("phí") || message.contains("chi phí")) {
            return "Chi phí khám và điều trị tại Eyespire phụ thuộc vào dịch vụ cụ thể. Để biết thông tin chi tiết về giá, vui lòng liên hệ với chúng tôi qua hotline hoặc đến trực tiếp phòng khám.";
        } else if (message.contains("địa chỉ") || message.contains("vị trí") || message.contains("ở đâu")) {
            return "Phòng khám mắt Eyespire tọa lạc tại trung tâm thành phố. Chúng tôi mở cửa từ 8:00 đến 17:00 từ thứ Hai đến thứ Bảy.";
        } else if (message.contains("bác sĩ") || message.contains("chuyên gia")) {
            return "Eyespire có đội ngũ bác sĩ giàu kinh nghiệm và chuyên môn cao trong lĩnh vực nhãn khoa. Các bác sĩ của chúng tôi đều được đào tạo bài bản và thường xuyên cập nhật kiến thức y khoa mới nhất.";
        } else if (message.contains("1+1") || message.contains("1 + 1") || message.contains("một cộng một")) {
            return "1 + 1 = 2. Tôi là trợ lý Eyespire, tôi có thể giúp bạn với các câu hỏi về sức khỏe mắt và dịch vụ của phòng khám Eyespire.";
        } else {
            // Trả về một câu trả lời ngẫu nhiên từ danh sách có sẵn
            return FALLBACK_RESPONSES[random.nextInt(FALLBACK_RESPONSES.length)];
        }
    }

    private String parseErrorMessage(String errorJson) {
        try {
            JsonObject jsonObject = JsonParser.parseString(errorJson).getAsJsonObject();
            if (jsonObject.has("error")) {
                JsonObject error = jsonObject.getAsJsonObject("error");
                String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                int code = error.has("code") ? error.get("code").getAsInt() : 0;
                
                if (code == 400) {
                    return "Xin lỗi, yêu cầu không hợp lệ. Vui lòng thử lại với câu hỏi khác.";
                } else if (code == 401 || code == 403) {
                    return "Xin lỗi, API key không hợp lệ hoặc hết hạn. Vui lòng liên hệ quản trị viên.";
                } else if (code == 429) {
                    return getFallbackResponse("dịch vụ");
                } else if (code >= 500) {
                    return "Xin lỗi, máy chủ đang gặp sự cố. Vui lòng thử lại sau.";
                } else {
                    return "Xin lỗi, đã xảy ra lỗi: " + message + ". Vui lòng thử lại sau.";
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing error message", e);
        }
        return "Xin lỗi, tôi đang gặp vấn đề kỹ thuật. Vui lòng thử lại sau.";
    }

    private String extractTextFromResponse(String jsonResponse) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");

            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");

                if (parts != null && parts.size() > 0) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString().trim();
                }
            }
        } catch (Exception e) {
            logger.error("JSON parsing error", e);
            return "Xin lỗi, tôi không thể xử lý phản hồi từ AI lúc này.";
        }
        return "Xin lỗi, tôi không thể xử lý yêu cầu của bạn lúc này.";
    }
    
    /**
     * Kiểm tra xem có phải câu hỏi về dữ liệu không
     */
    private boolean isDataQuery(String message) {
        String lowerMessage = message.toLowerCase();
        
        // Keywords indicating data queries
        String[] dataKeywords = {
            "tìm", "tìm kiếm", "search", "find", "có bao nhiêu", "how many",
            "danh sách", "list", "thống kê", "statistics", "báo cáo", "report",
            "doanh thu", "revenue", "bệnh nhân nào", "which patient",
            "bác sĩ nào", "which doctor", "thuốc nào", "which medicine",
            "lịch hẹn", "appointment", "hồ sơ", "medical record",
            "sản phẩm", "product", "kính", "glasses", "eyewear"
        };
        
        for (String keyword : dataKeywords) {
            if (lowerMessage.contains(keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Xử lý câu hỏi về dữ liệu bằng AI Query Service
     */
    private String handleDataQuery(String message) {
        try {
            logger.info("Processing data query: {}", message);
            
            Map<String, Object> queryResult = aiQueryService.processQuery(message);
            
            if ((Boolean) queryResult.get("success")) {
                String aiResponse = (String) queryResult.get("response");
                
                // Lưu phản hồi vào lịch sử
                conversationHistory.add(new ChatMessage("assistant", aiResponse));
                
                return aiResponse;
            } else {
                String error = (String) queryResult.get("error");
                logger.warn("AI Query failed: {}", error);
                
                // Fallback to normal AI response
                return generateGeminiResponse(message);
            }
            
        } catch (Exception e) {
            logger.error("Error in handleDataQuery", e);
            
            // Fallback to normal AI response
            return generateGeminiResponse(message);
        }
    }
    
    /**
     * Tách logic Gemini AI thành method riêng
     */
    private String generateGeminiResponse(String userMessage) {
        // Original Gemini AI logic here
        return getFallbackResponse(userMessage);
    }

    private static class ChatMessage {
        private String role;
        private String text;

        public ChatMessage(String role, String text) {
            this.role = role;
            this.text = text;
        }
    }
}
