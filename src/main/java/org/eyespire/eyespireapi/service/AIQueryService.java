package org.eyespire.eyespireapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class AIQueryService {

    private static final Logger logger = LoggerFactory.getLogger(AIQueryService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private AppointmentService appointmentService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;

    // Query routing patterns - Semantic keywords
    private final Map<String, List<String>> semanticKeywords = Map.of(
        "APPOINTMENT", Arrays.asList("lịch hẹn", "cuộc hẹn", "appointment", "đặt lịch", "hẹn khám", "booking", "schedule"),
        "MEDICAL_RECORD", Arrays.asList("hồ sơ", "bệnh án", "chẩn đoán", "triệu chứng", "medical record", "diagnosis", "khám bệnh", "điều trị"),
        "PRODUCT", Arrays.asList("thuốc", "kính", "sản phẩm", "product", "medicine", "eyewear", "mua", "bán", "giá"),
        "USER", Arrays.asList("bác sĩ", "bệnh nhân", "nhân viên", "doctor", "patient", "user", "staff", "người dùng"),
        "ANALYTICS", Arrays.asList("doanh thu", "thống kê", "báo cáo", "analytics", "revenue", "report", "số liệu", "tổng kết")
    );

    // Date patterns for time-based queries
    private final Pattern datePattern = Pattern.compile("(?i)(hôm nay|today|tuần này|this week|tháng này|this month|năm này|this year|\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");
    
    // Number patterns for quantity/price queries
    private final Pattern numberPattern = Pattern.compile("\\d+");

    /**
     * Main method for processing natural language queries
     */
    public Map<String, Object> processQuery(String query) {
        logger.info("Processing AI query: {}", query);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. Query Routing - Determine query type
            String queryType = routeQuery(query);
            result.put("queryType", queryType);
            
            // 2. Query Transform - Extract parameters
            Map<String, Object> parameters = extractParameters(query, queryType);
            result.put("parameters", parameters);
            
            // 3. Execute query based on type
            Object data = executeQuery(queryType, parameters);
            result.put("data", data);
            
            // 4. Generate natural language response
            String response = generateNaturalResponse(queryType, parameters, data);
            result.put("response", response);
            
            result.put("success", true);
            
        } catch (Exception e) {
            logger.error("Error processing query: {}", query, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("response", "Xin lỗi, tôi không thể xử lý câu hỏi này. Vui lòng thử lại với cách diễn đạt khác.");
        }
        
        return result;
    }

    /**
     * Query Routing - Determine the type of query using semantic analysis
     */
    private String routeQuery(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        
        // Calculate semantic similarity scores
        Map<String, Integer> scores = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : semanticKeywords.entrySet()) {
            String category = entry.getKey();
            List<String> keywords = entry.getValue();
            
            int score = 0;
            for (String keyword : keywords) {
                if (normalizedQuery.contains(keyword)) {
                    score += keyword.length(); // Longer matches get higher scores
                }
            }
            scores.put(category, score);
        }
        
        // Return category with highest score
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("GENERAL");
    }

    /**
     * Query Transform - Extract parameters from natural language
     */
    private Map<String, Object> extractParameters(String query, String queryType) {
        Map<String, Object> params = new HashMap<>();
        String normalizedQuery = query.toLowerCase();
        
        // Extract date/time parameters
        Matcher dateMatcher = datePattern.matcher(normalizedQuery);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1);
            LocalDate date = parseDate(dateStr);
            params.put("date", date);
            params.put("dateRange", getDateRange(dateStr));
        }
        
        // Extract numbers (quantities, prices, IDs)
        Matcher numberMatcher = numberPattern.matcher(query);
        List<Integer> numbers = new ArrayList<>();
        while (numberMatcher.find()) {
            numbers.add(Integer.parseInt(numberMatcher.group()));
        }
        if (!numbers.isEmpty()) {
            params.put("numbers", numbers);
        }
        
        // Extract specific entities based on query type
        switch (queryType) {
            case "USER":
                extractUserParameters(normalizedQuery, params);
                break;
            case "PRODUCT":
                extractProductParameters(normalizedQuery, params);
                break;
            case "MEDICAL_RECORD":
                extractMedicalParameters(normalizedQuery, params);
                break;
            case "APPOINTMENT":
                extractAppointmentParameters(normalizedQuery, params);
                break;
        }
        
        return params;
    }

    /**
     * Execute query based on type and parameters
     */
    private Object executeQuery(String queryType, Map<String, Object> parameters) {
        switch (queryType) {
            case "APPOINTMENT":
                return executeAppointmentQuery(parameters);
            case "MEDICAL_RECORD":
                return executeMedicalRecordQuery(parameters);
            case "PRODUCT":
                return executeProductQuery(parameters);
            case "USER":
                return executeUserQuery(parameters);
            case "ANALYTICS":
                return executeAnalyticsQuery(parameters);
            default:
                return "Tôi có thể giúp bạn tìm kiếm thông tin về lịch hẹn, hồ sơ bệnh án, sản phẩm, người dùng và báo cáo thống kê.";
        }
    }

    /**
     * Execute appointment-related queries
     */
    private Object executeAppointmentQuery(Map<String, Object> parameters) {
        try {
            StringBuilder sql = new StringBuilder("SELECT TOP 10 a.*, u.name as patient_name, d.name as doctor_name FROM appointments a ");
            sql.append("LEFT JOIN users u ON a.patient_id = u.id ");
            sql.append("LEFT JOIN doctors doc ON a.doctor_id = doc.id ");
            sql.append("LEFT JOIN users d ON doc.user_id = d.id WHERE 1=1 ");
            
            List<Object> params = new ArrayList<>();
            
            // Add date filter
            if (parameters.containsKey("date")) {
                sql.append("AND DATE(a.appointment_date) = ? ");
                params.add(parameters.get("date"));
            }
            
            // Add status filter
            sql.append("ORDER BY a.appointment_date DESC");
            
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
        } catch (Exception e) {
            logger.error("Error executing appointment query", e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute medical record queries
     */
    private Object executeMedicalRecordQuery(Map<String, Object> parameters) {
        try {
            StringBuilder sql = new StringBuilder("SELECT TOP 10 mr.*, u.name as patient_name FROM medical_records mr ");
            sql.append("LEFT JOIN appointments a ON mr.appointment_id = a.id ");
            sql.append("LEFT JOIN users u ON a.patient_id = u.id WHERE 1=1 ");
            
            List<Object> params = new ArrayList<>();
            
            // Add symptom search
            if (parameters.containsKey("symptoms")) {
                sql.append("AND (mr.diagnosis LIKE ? OR mr.notes LIKE ?) ");
                String symptom = "%" + parameters.get("symptoms") + "%";
                params.add(symptom);
                params.add(symptom);
            }
            
            sql.append("ORDER BY mr.created_at DESC");
            
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
        } catch (Exception e) {
            logger.error("Error executing medical record query", e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute product queries
     */
    private Object executeProductQuery(Map<String, Object> parameters) {
        try {
            StringBuilder sql = new StringBuilder("SELECT TOP 10 * FROM products WHERE 1=1 ");
            List<Object> params = new ArrayList<>();
            
            // Add product type filter
            if (parameters.containsKey("productType")) {
                sql.append("AND type = ? ");
                params.add(parameters.get("productType"));
            }
            
            // Add name search
            if (parameters.containsKey("productName")) {
                sql.append("AND name LIKE ? ");
                params.add("%" + parameters.get("productName") + "%");
            }
            
            sql.append("ORDER BY name");
            
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
        } catch (Exception e) {
            logger.error("Error executing product query", e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute user queries
     */
    private Object executeUserQuery(Map<String, Object> parameters) {
        try {
            StringBuilder sql = new StringBuilder("SELECT TOP 10 * FROM users WHERE 1=1 ");
            List<Object> params = new ArrayList<>();
            
            // Add role filter
            if (parameters.containsKey("role")) {
                sql.append("AND role = ? ");
                params.add(parameters.get("role"));
            }
            
            // Add name search
            if (parameters.containsKey("userName")) {
                sql.append("AND name LIKE ? ");
                params.add("%" + parameters.get("userName") + "%");
            }
            
            sql.append("ORDER BY name");
            
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
        } catch (Exception e) {
            logger.error("Error executing user query", e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute analytics queries
     */
    private Object executeAnalyticsQuery(Map<String, Object> parameters) {
        try {
            Map<String, Object> analytics = new HashMap<>();
            
            // Get appointment statistics
            String appointmentSql = "SELECT TOP 10 COUNT(*) as total, status FROM appointments GROUP BY status";
            List<Map<String, Object>> appointmentStats = jdbcTemplate.queryForList(appointmentSql);
            analytics.put("appointments", appointmentStats);
            
            // Get revenue statistics (if date range specified)
            if (parameters.containsKey("dateRange")) {
                String revenueSql = "SELECT TOP 10 SUM(total_amount) as revenue FROM orders WHERE created_at >= ? AND created_at <= ?";
                // Add date range logic here
            }
            
            return analytics;
            
        } catch (Exception e) {
            logger.error("Error executing analytics query", e);
            return Collections.emptyMap();
        }
    }

    // Helper methods for parameter extraction
    private void extractUserParameters(String query, Map<String, Object> params) {
        if (query.contains("bác sĩ") || query.contains("doctor")) {
            params.put("role", "DOCTOR");
        } else if (query.contains("bệnh nhân") || query.contains("patient")) {
            params.put("role", "PATIENT");
        }
    }

    private void extractProductParameters(String query, Map<String, Object> params) {
        if (query.contains("thuốc") || query.contains("medicine")) {
            params.put("productType", "MEDICINE");
        } else if (query.contains("kính") || query.contains("eyewear")) {
            params.put("productType", "EYEWEAR");
        }
    }

    private void extractMedicalParameters(String query, Map<String, Object> params) {
        // Extract common symptoms
        if (query.contains("mờ mắt") || query.contains("blurry")) {
            params.put("symptoms", "mờ mắt");
        } else if (query.contains("đau mắt") || query.contains("eye pain")) {
            params.put("symptoms", "đau mắt");
        }
    }

    private void extractAppointmentParameters(String query, Map<String, Object> params) {
        // Extract appointment status
        if (query.contains("hoàn thành") || query.contains("completed")) {
            params.put("status", "COMPLETED");
        } else if (query.contains("hủy") || query.contains("cancelled")) {
            params.put("status", "CANCELLED");
        }
    }

    // Helper methods for date parsing
    private LocalDate parseDate(String dateStr) {
        LocalDate today = LocalDate.now();
        
        switch (dateStr.toLowerCase()) {
            case "hôm nay":
            case "today":
                return today;
            case "tuần này":
            case "this week":
                return today.with(java.time.DayOfWeek.MONDAY);
            case "tháng này":
            case "this month":
                return today.withDayOfMonth(1);
            default:
                // Try to parse specific date formats
                try {
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception e) {
                    return today;
                }
        }
    }

    private String getDateRange(String dateStr) {
        switch (dateStr.toLowerCase()) {
            case "tuần này":
            case "this week":
                return "WEEK";
            case "tháng này":
            case "this month":
                return "MONTH";
            case "năm này":
            case "this year":
                return "YEAR";
            default:
                return "DAY";
        }
    }

    /**
     * Generate natural language response based on query results
     */
    private String generateNaturalResponse(String queryType, Map<String, Object> parameters, Object data) {
        switch (queryType) {
            case "APPOINTMENT":
                return generateAppointmentResponse(parameters, (List<Map<String, Object>>) data);
            case "MEDICAL_RECORD":
                return generateMedicalRecordResponse(parameters, (List<Map<String, Object>>) data);
            case "PRODUCT":
                return generateProductResponse(parameters, (List<Map<String, Object>>) data);
            case "USER":
                return generateUserResponse(parameters, (List<Map<String, Object>>) data);
            case "ANALYTICS":
                return generateAnalyticsResponse(parameters, (Map<String, Object>) data);
            default:
                return "Tôi đã tìm thấy thông tin bạn yêu cầu. Bạn có muốn biết thêm chi tiết gì không?";
        }
    }

    private String generateAppointmentResponse(Map<String, Object> parameters, List<Map<String, Object>> appointments) {
        if (appointments.isEmpty()) {
            return "Không tìm thấy lịch hẹn nào phù hợp với yêu cầu của bạn.";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("Tôi tìm thấy ").append(appointments.size()).append(" lịch hẹn:\n\n");
        
        for (int i = 0; i < Math.min(appointments.size(), 5); i++) {
            Map<String, Object> appointment = appointments.get(i);
            response.append("• ")
                .append(appointment.get("patient_name"))
                .append(" - ")
                .append(appointment.get("appointment_date"))
                .append(" (")
                .append(appointment.get("status"))
                .append(")\n");
        }
        
        if (appointments.size() > 5) {
            response.append("\n... và ").append(appointments.size() - 5).append(" lịch hẹn khác.");
        }
        
        return response.toString();
    }

    private String generateMedicalRecordResponse(Map<String, Object> parameters, List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return "Không tìm thấy hồ sơ bệnh án nào phù hợp.";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("Tìm thấy ").append(records.size()).append(" hồ sơ bệnh án:\n\n");
        
        for (int i = 0; i < Math.min(records.size(), 3); i++) {
            Map<String, Object> record = records.get(i);
            response.append("• Bệnh nhân: ").append(record.get("patient_name"))
                .append("\n  Chẩn đoán: ").append(record.get("diagnosis"))
                .append("\n\n");
        }
        
        return response.toString();
    }

    private String generateProductResponse(Map<String, Object> parameters, List<Map<String, Object>> products) {
        if (products.isEmpty()) {
            return "Không tìm thấy sản phẩm nào phù hợp.";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("Tìm thấy ").append(products.size()).append(" sản phẩm:\n\n");
        
        for (int i = 0; i < Math.min(products.size(), 5); i++) {
            Map<String, Object> product = products.get(i);
            response.append("• ").append(product.get("name"))
                .append(" - ").append(product.get("price")).append(" VNĐ")
                .append(" (").append(product.get("type")).append(")\n");
        }
        
        return response.toString();
    }

    private String generateUserResponse(Map<String, Object> parameters, List<Map<String, Object>> users) {
        if (users.isEmpty()) {
            return "Không tìm thấy người dùng nào phù hợp.";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("Tìm thấy ").append(users.size()).append(" người dùng:\n\n");
        
        for (int i = 0; i < Math.min(users.size(), 5); i++) {
            Map<String, Object> user = users.get(i);
            response.append("• ").append(user.get("name"))
                .append(" (").append(user.get("role")).append(")")
                .append(" - ").append(user.get("email")).append("\n");
        }
        
        return response.toString();
    }

    private String generateAnalyticsResponse(Map<String, Object> parameters, Map<String, Object> analytics) {
        StringBuilder response = new StringBuilder();
        response.append("📊 Báo cáo thống kê:\n\n");
        
        if (analytics.containsKey("appointments")) {
            List<Map<String, Object>> appointmentStats = (List<Map<String, Object>>) analytics.get("appointments");
            response.append("📅 Lịch hẹn:\n");
            for (Map<String, Object> stat : appointmentStats) {
                response.append("• ").append(stat.get("status"))
                    .append(": ").append(stat.get("total")).append(" cuộc hẹn\n");
            }
        }
        
        return response.toString();
    }
}
