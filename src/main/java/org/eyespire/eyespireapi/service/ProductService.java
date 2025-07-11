package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.ProductDTO;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.enums.ProductType;
import org.eyespire.eyespireapi.repository.OrderItemRepository;
import org.eyespire.eyespireapi.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public List<ProductDTO> getAllProducts() {
        // Fetch all products
        List<Product> products = productRepository.findAll();

        // Fetch sales data for all products
        List<Map<String, Object>> salesData = orderItemRepository.findTotalSalesByProduct();
        Map<Integer, Long> salesMap = new HashMap<>();
        for (Map<String, Object> data : salesData) {
            Integer productId = (Integer) data.get("productId");
            Long totalQuantity = ((Number) data.get("totalQuantity")).longValue();
            salesMap.put(productId, totalQuantity);
        }

        // Map products to DTOs with sales data
        return products.stream()
                .map(product -> ProductDTO.fromEntity(product, salesMap.getOrDefault(product.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Fetch sales data for the specific product
        List<Map<String, Object>> salesData = orderItemRepository.findTotalSalesByProduct();
        Long sales = salesData.stream()
                .filter(data -> data.get("productId").equals(id))
                .map(data -> ((Number) data.get("totalQuantity")).longValue())
                .findFirst()
                .orElse(0L);

        return ProductDTO.fromEntity(product, sales);
    }

    public List<ProductDTO> getProductsByType(ProductType type) {
        List<Product> products = productRepository.findAllByType(type);

        // Fetch sales data for all products
        List<Map<String, Object>> salesData = orderItemRepository.findTotalSalesByProduct();
        Map<Integer, Long> salesMap = new HashMap<>();
        for (Map<String, Object> data : salesData) {
            Integer productId = (Integer) data.get("productId");
            Long totalQuantity = ((Number) data.get("totalQuantity")).longValue();
            salesMap.put(productId, totalQuantity);
        }

        return products.stream()
                .map(product -> ProductDTO.fromEntity(product, salesMap.getOrDefault(product.getId(), 0L)))
                .collect(Collectors.toList());
    }

    public ProductDTO createProduct(Product product) {
        Product savedProduct = productRepository.save(product);
        return ProductDTO.fromEntity(savedProduct, 0L); // New product has no sales yet
    }

    public ProductDTO updateProduct(Integer id, Product product) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setId(id);
        if (product.getImageUrl() == null) {
            product.setImageUrl(existingProduct.getImageUrl());
        }

        Product updatedProduct = productRepository.save(product);

        // Fetch sales data for the updated product
        List<Map<String, Object>> salesData = orderItemRepository.findTotalSalesByProduct();
        Long sales = salesData.stream()
                .filter(data -> data.get("productId").equals(id))
                .map(data -> ((Number) data.get("totalQuantity")).longValue())
                .findFirst()
                .orElse(0L);

        return ProductDTO.fromEntity(updatedProduct, sales);
    }

    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        productRepository.deleteById(id);
    }

    public ProductDTO updateProductStock(Integer id, Integer newQuantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        product.setStockQuantity(newQuantity);
        Product updatedProduct = productRepository.save(product);

        // Fetch sales data for the updated product
        List<Map<String, Object>> salesData = orderItemRepository.findTotalSalesByProduct();
        Long sales = salesData.stream()
                .filter(data -> data.get("productId").equals(id))
                .map(data -> ((Number) data.get("totalQuantity")).longValue())
                .findFirst()
                .orElse(0L);

        return ProductDTO.fromEntity(updatedProduct, sales);
    }

    public Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());
        product.setImageUrl(dto.getImageUrl());
        product.setCreatedAt(dto.getCreatedAt());
        product.setUpdatedAt(dto.getUpdatedAt());
        product.setType(dto.getType());
        return product;
    }
}