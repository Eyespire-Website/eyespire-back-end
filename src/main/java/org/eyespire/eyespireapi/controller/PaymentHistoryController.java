package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.PaymentHistoryDTO;
import org.eyespire.eyespireapi.service.PaymentHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment-history")
@CrossOrigin(origins = "https://eyespire.vercel.app")
public class PaymentHistoryController {

    @Autowired
    private PaymentHistoryService paymentHistoryService;

    /**
     * Lấy lịch sử thanh toán của người dùng
     * @param userId ID của người dùng
     * @return Danh sách lịch sử thanh toán
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentHistoryDTO>> getUserPaymentHistory(@PathVariable Integer userId) {
        try {
            List<PaymentHistoryDTO> paymentHistory = paymentHistoryService.getUserPaymentHistory(userId);
            return ResponseEntity.ok(paymentHistory);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy lịch sử thanh toán: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy chi tiết hóa đơn
     * @param id ID của hóa đơn
     * @param type Loại hóa đơn (service hoặc order)
     * @return Chi tiết hóa đơn
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentHistoryDTO> getPaymentDetail(
            @PathVariable String id, 
            @RequestParam String type) {
        try {
            PaymentHistoryDTO paymentDetail = paymentHistoryService.getPaymentDetail(id, type);
            if (paymentDetail == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(paymentDetail);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage(), e);
        }
    }

    /**
     * Lọc lịch sử thanh toán theo loại
     * @param userId ID của người dùng
     * @param type Loại hóa đơn (service hoặc order)
     * @return Danh sách lịch sử thanh toán đã lọc
     */
    @GetMapping("/user/{userId}/filter")
    public ResponseEntity<List<PaymentHistoryDTO>> filterPaymentHistory(
            @PathVariable Integer userId,
            @RequestParam(required = false) String type) {
        try {
            List<PaymentHistoryDTO> filteredHistory = paymentHistoryService.filterPaymentHistory(userId, type);
            return ResponseEntity.ok(filteredHistory);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Lỗi khi lọc lịch sử thanh toán: " + e.getMessage(), e);
        }
    }

    /**
     * Tìm kiếm lịch sử thanh toán
     * @param userId ID của người dùng
     * @param query Từ khóa tìm kiếm
     * @return Danh sách lịch sử thanh toán phù hợp với từ khóa
     */
    @GetMapping("/user/{userId}/search")
    public ResponseEntity<List<PaymentHistoryDTO>> searchPaymentHistory(
            @PathVariable Integer userId,
            @RequestParam String query) {
        try {
            List<PaymentHistoryDTO> searchResults = paymentHistoryService.searchPaymentHistory(userId, query);
            return ResponseEntity.ok(searchResults);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Lỗi khi tìm kiếm lịch sử thanh toán: " + e.getMessage(), e);
        }
    }
}
