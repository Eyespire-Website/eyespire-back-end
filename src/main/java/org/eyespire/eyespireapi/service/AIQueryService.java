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
    private final Map<String, List<String>> semanticKeywords;
    
    {
        semanticKeywords = new HashMap<>();
        semanticKeywords.put("APPOINTMENT", Arrays.asList("lịch hẹn", "cuộc hẹn", "appointment", "đặt lịch", "hẹn khám", "booking", "schedule"));
        semanticKeywords.put("MEDICAL_RECORD", Arrays.asList("hồ sơ", "bệnh án", "chẩn đoán", "triệu chứng", "medical record", "diagnosis", "khám bệnh", "điều trị"));
        semanticKeywords.put("ORDER", Arrays.asList("đơn hàng", "order", "mua hàng", "purchase", "giao hàng", "delivery", "thanh toán", "payment", "hoá đơn", "invoice", "mua", "buy", "gần đây", "recent"));
        semanticKeywords.put("PRODUCT", Arrays.asList("thuốc", "kính", "sản phẩm", "product", "medicine", "eyewear", "mua", "bán", "giá"));
        semanticKeywords.put("USER", Arrays.asList("bác sĩ", "bệnh nhân", "nhân viên", "doctor", "patient", "user", "staff", "người dùng"));
        semanticKeywords.put("ANALYTICS", Arrays.asList("doanh thu", "thống kê", "báo cáo", "analytics", "revenue", "report", "số liệu", "tổng kết"));
    }

    // Date patterns for time-based queries
    private final Pattern datePattern = Pattern.compile("(?i)(hôm nay|today|tuần này|this week|tháng này|this month|năm này|this year|\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})");
    
    // Number patterns for extracting quantities, prices, IDs
    private final Pattern numberPattern = Pattern.compile("\\d+");

    /**
     * Main entry point for processing queries
     */
    public Map<String, Object> processQuery(String query, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Determine query type
            String queryType = routeQuery(query);
            
            // Extract parameters
            Map<String, Object> parameters = extractParameters(query, queryType);
            parameters.put("userId", userId);
            
            // Execute query
            Object data = executeQuery(queryType, parameters);
            
            // Generate natural language response
            String response = generateNaturalResponse(queryType, parameters, data);
            
            result.put("success", true);
            result.put("response", response);
            result.put("queryType", queryType);
            result.put("data", data);
            
        } catch (Exception e) {
            logger.error("Error processing query: {}", query, e);
            result.put("success", false);
            result.put("error", "Có lỗi xảy ra khi xử lý truy vấn. Vui lòng thử lại.");
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
            case "ORDER":
                extractOrderParameters(normalizedQuery, params);
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
            case "ORDER":
                return executeOrderQuery(parameters);
            case "PRODUCT":
                return executeProductQuery(parameters);
            case "USER":
                return executeUserQuery(parameters);
            case "ANALYTICS":
                return executeAnalyticsQuery(parameters);
            default:
                return "Tôi có thể giúp bạn tìm kiếm thông tin về lịch hẹn, hồ sơ bệnh án, đơn hàng, sản phẩm, người dùng và báo cáo thống kê.";
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
                sql.append("AND CAST(a.appointment_time AS DATE) = ? ");
                params.add(parameters.get("date"));
            }
            
            // Add status filter
            sql.append("ORDER BY a.appointment_time DESC");
            
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
     * Execute order queries
     */
    private Object executeOrderQuery(Map<String, Object> parameters) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT TOP 10 o.id, o.total_amount, o.status, o.order_date, o.created_at, ");
            sql.append("o.shipping_address, COUNT(oi.id) as item_count ");
            sql.append("FROM orders o ");
            sql.append("LEFT JOIN order_items oi ON o.id = oi.order_id ");
            sql.append("WHERE 1=1 ");
            
            List<Object> params = new ArrayList<>();
            
            // Filter by patient ID (userId)
            if (parameters.containsKey("userId")) {
                sql.append("AND o.patient_id = ? ");
                params.add(parameters.get("userId"));
            }
            
            // Add date filter if specified
            if (parameters.containsKey("dateFrom")) {
                sql.append("AND o.order_date >= ? ");
                params.add(parameters.get("dateFrom"));
            }
            
            if (parameters.containsKey("dateTo")) {
                sql.append("AND o.order_date <= ? ");
                params.add(parameters.get("dateTo"));
            }
            
            // Add status filter if specified
            if (parameters.containsKey("status")) {
                sql.append("AND o.status = ? ");
                params.add(parameters.get("status"));
            }
            
            sql.append("GROUP BY o.id, o.total_amount, o.status, o.order_date, o.created_at, o.shipping_address ");
            sql.append("ORDER BY o.created_at DESC");
            
            return jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
        } catch (Exception e) {
            logger.error("Error executing order query", e);
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
    
    /**
     * Generate natural language response based on query results
     */
    private String generateNaturalResponse(String queryType, Map<String, Object> parameters, Object data) {
        switch (queryType) {
            case "APPOINTMENT":
                return generateAppointmentResponse(parameters, (List<Map<String, Object>>) data);
            case "MEDICAL_RECORD":
                return generateMedicalRecordResponse(parameters, (List<Map<String, Object>>) data);
            case "ORDER":
                return generateOrderResponse(parameters, (List<Map<String, Object>>) data);
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
    
    private String generateOrderResponse(Map<String, Object> parameters, List<Map<String, Object>> orders) {
        if (orders.isEmpty()) {
            return "📎 Không tìm thấy đơn hàng nào.";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("📎 **Đơn hàng của bạn (").append(orders.size()).append(" đơn hàng):**\n\n");
        
        for (int i = 0; i < Math.min(orders.size(), 5); i++) {
            Map<String, Object> order = orders.get(i);
            response.append("📎 **Đơn hàng #").append(order.get("id")).append(":**\n");
            
            if (order.get("total_amount") != null) {
                response.append("💰 **Tổng tiền:** ").append(String.format("%,.0f VNĐ", order.get("total_amount"))).append("\n");
            }
            
            if (order.get("status") != null) {
                String status = order.get("status").toString();
                String statusEmoji = getOrderStatusEmoji(status);
                response.append(statusEmoji).append(" **Trạng thái:** ").append(getOrderStatusText(status)).append("\n");
            }
            
            if (order.get("item_count") != null) {
                response.append("📦 **Số sản phẩm:** ").append(order.get("item_count")).append("\n");
            }
            
            if (order.get("order_date") != null) {
                response.append("📅 **Ngày đặt:** ").append(order.get("order_date")).append("\n");
            }
            
            response.append("\n");
        }
        
        if (orders.size() > 5) {
            response.append("📄 **Và ").append(orders.size() - 5).append(" đơn hàng khác...**\n\n");
        }
        
        response.append("📞 **Lưu ý:** Nếu cần hỗ trợ về đơn hàng, hãy liên hệ chúng tôi.");
        
        return response.toString();
    }
    
    private String getOrderStatusEmoji(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "⏳";
            case "CONFIRMED":
                return "✅";
            case "PROCESSING":
                return "🔄";
            case "SHIPPED":
                return "🚚";
            case "DELIVERED":
                return "🎉";
            case "CANCELLED":
                return "❌";
            default:
                return "📎";
        }
    }
    
    private String getOrderStatusText(String status) {
        switch (status.toUpperCase()) {
            case "PENDING":
                return "Chờ xác nhận";
            case "CONFIRMED":
                return "Đã xác nhận";
            case "PROCESSING":
                return "Đang xử lý";
            case "SHIPPED":
                return "Đang giao hàng";
            case "DELIVERED":
                return "Đã giao hàng";
            case "CANCELLED":
                return "Đã hủy";
            default:
                return status;
        }
    }
    
    private void extractOrderParameters(String query, Map<String, Object> params) {
        // Extract order status
        if (query.contains("chờ") || query.contains("pending")) {
            params.put("status", "PENDING");
        } else if (query.contains("xác nhận") || query.contains("confirmed")) {
            params.put("status", "CONFIRMED");
        } else if (query.contains("xử lý") || query.contains("processing")) {
            params.put("status", "PROCESSING");
        } else if (query.contains("giao") || query.contains("shipped")) {
            params.put("status", "SHIPPED");
        } else if (query.contains("hoàn thành") || query.contains("delivered")) {
            params.put("status", "DELIVERED");
        } else if (query.contains("hủy") || query.contains("cancelled")) {
            params.put("status", "CANCELLED");
        }
        
        // Extract time range
        if (query.contains("gần đây") || query.contains("recent")) {
            params.put("timeRange", "RECENT");
        } else if (query.contains("tuần này") || query.contains("this week")) {
            params.put("timeRange", "THIS_WEEK");
        } else if (query.contains("tháng này") || query.contains("this month")) {
            params.put("timeRange", "THIS_MONTH");
        } else if (query.contains("năm này") || query.contains("this year")) {
            params.put("timeRange", "THIS_YEAR");
        }
    }
}
