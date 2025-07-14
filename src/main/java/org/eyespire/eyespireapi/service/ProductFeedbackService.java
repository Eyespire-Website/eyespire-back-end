package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.ProductFeedbackDTO;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.ProductFeedback;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.repository.ProductFeedbackRepository;
import org.eyespire.eyespireapi.repository.ProductRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductFeedbackService {

    private final ProductFeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Autowired
    public ProductFeedbackService(
            ProductFeedbackRepository feedbackRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<ProductFeedbackDTO> getFeedbackByProductId(Integer productId) {
        return feedbackRepository.findByProductId(productId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProductFeedbackDTO createFeedback(ProductFeedbackDTO feedbackDTO) {
        Product product = productRepository.findById(feedbackDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + feedbackDTO.getProductId()));

        User patient = userRepository.findById(feedbackDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + feedbackDTO.getPatientId()));

        ProductFeedback feedback = new ProductFeedback();
        feedback.setProduct(product);
        feedback.setPatient(patient);
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());
        feedback.setCreatedAt(feedbackDTO.getCreatedAt());

        ProductFeedback savedFeedback = feedbackRepository.save(feedback);
        return convertToDTO(savedFeedback);
    }

    public ProductFeedbackDTO updateFeedback(ProductFeedbackDTO feedbackDTO) {
        ProductFeedback feedback = feedbackRepository.findById(feedbackDTO.getId())
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + feedbackDTO.getId()));

        Product product = productRepository.findById(feedbackDTO.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + feedbackDTO.getProductId()));

        User patient = userRepository.findById(feedbackDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + feedbackDTO.getPatientId()));

        feedback.setProduct(product);
        feedback.setPatient(patient);
        feedback.setRating(feedbackDTO.getRating());
        feedback.setComment(feedbackDTO.getComment());
        // Preserve original createdAt date
        // Optionally update other fields as needed

        ProductFeedback updatedFeedback = feedbackRepository.save(feedback);
        return convertToDTO(updatedFeedback);
    }


    public void deleteFeedback(Integer feedbackId) {
        ProductFeedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + feedbackId));
        feedbackRepository.delete(feedback);
    }

    private ProductFeedbackDTO convertToDTO(ProductFeedback feedback) {
        ProductFeedbackDTO dto = new ProductFeedbackDTO();
        dto.setId(feedback.getId());
        dto.setProductId(feedback.getProduct().getId());
        dto.setPatientId(feedback.getPatient().getId());
        dto.setPatientName(feedback.getPatient().getName());
        dto.setRating(feedback.getRating());
        dto.setComment(feedback.getComment());
        dto.setCreatedAt(feedback.getCreatedAt());
        dto.setVerified(true); // Assume verified for simplicity
        return dto;
    }
}