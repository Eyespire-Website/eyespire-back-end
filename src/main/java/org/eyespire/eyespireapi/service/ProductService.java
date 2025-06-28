package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.ProductDTO;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.enums.ProductType;
import org.eyespire.eyespireapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Integer id) {
        return productRepository.findById(id)
                .map(ProductDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }

    public List<ProductDTO> getProductsByType(ProductType type) {
        return productRepository.findAllByType(type).stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public ProductDTO createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        return ProductDTO.fromEntity(savedProduct);
    }

    // Phương thức này không còn sử dụng StorageService
    // Thay vào đó, imageUrl sẽ được thiết lập trước khi gọi phương thức này

    public ProductDTO updateProduct(Integer id, Product product) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setId(id);
        if (existingProduct.getImageUrl() != null) {
            product.setImageUrl(existingProduct.getImageUrl());
        }
        
        Product updatedProduct = productRepository.save(product);
        return ProductDTO.fromEntity(updatedProduct);
    }

    // Phương thức này không còn sử dụng StorageService
    // imageUrl sẽ được thiết lập trước khi gọi phương thức này

    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Không cần xóa ảnh nữa vì không sử dụng StorageService
        // Việc quản lý file sẽ được xử lý ở nơi khác
        
        productRepository.deleteById(id);
    }

    public ProductDTO updateProductStock(Integer id, Integer newQuantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setStockQuantity(newQuantity);
        Product updatedProduct = productRepository.save(product);
        
        return ProductDTO.fromEntity(updatedProduct);
    }
}
