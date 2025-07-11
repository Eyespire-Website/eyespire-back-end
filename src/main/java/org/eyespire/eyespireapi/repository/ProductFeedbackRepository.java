package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.ProductFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductFeedbackRepository extends JpaRepository<ProductFeedback, Integer> {
    List<ProductFeedback> findByProductId(Integer productId);
}