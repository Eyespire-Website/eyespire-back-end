package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.ProductFeedbackDTO;
import org.eyespire.eyespireapi.service.ProductFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
@CrossOrigin(origins = "*")
public class ProductFeedbackController {

    private final ProductFeedbackService feedbackService;

    @Autowired
    public ProductFeedbackController(ProductFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductFeedbackDTO>> getFeedbackByProductId(@PathVariable Integer productId) {
        List<ProductFeedbackDTO> feedbacks = feedbackService.getFeedbackByProductId(productId);
        return ResponseEntity.ok(feedbacks);
    }

    @PostMapping
    public ResponseEntity<ProductFeedbackDTO> createFeedback(@RequestBody ProductFeedbackDTO feedbackDTO) {
        ProductFeedbackDTO createdFeedback = feedbackService.createFeedback(feedbackDTO);
        return new ResponseEntity<>(createdFeedback, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductFeedbackDTO> updateFeedback(@PathVariable Integer id, @RequestBody ProductFeedbackDTO feedbackDTO) {
        feedbackDTO.setId(id);
        ProductFeedbackDTO updatedFeedback = feedbackService.updateFeedback(feedbackDTO);
        return ResponseEntity.ok(updatedFeedback);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable Integer id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }
}