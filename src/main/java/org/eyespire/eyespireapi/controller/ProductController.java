package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.ProductDTO;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.enums.ProductType;
import org.eyespire.eyespireapi.service.FileStorageService;
import org.eyespire.eyespireapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;

    @Autowired
    public ProductController(ProductService productService, FileStorageService fileStorageService) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Integer id) {
        ProductDTO product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<ProductDTO>> getProductsByType(@PathVariable ProductType type) {
        List<ProductDTO> products = productService.getProductsByType(type);
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody Product product) {
        ProductDTO createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    /**
     * API để tải lên ảnh sản phẩm
     * @param image File ảnh cần upload
     * @return URL của ảnh đã upload
     */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadProductImage(
            @RequestParam("image") MultipartFile image) {
        
        // Kiểm tra file
        if (image.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể upload file trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Kiểm tra định dạng file
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Chỉ chấp nhận file ảnh");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Lưu file vào thư mục gốc (không sử dụng thư mục con)
            String imageUrl = fileStorageService.storeImage(image, "");
            
            // Trả về URL của ảnh
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể lưu file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable Integer id,
            @RequestBody Product product) {
        
        ProductDTO updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<ProductDTO> updateProductStock(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> stockUpdate) {
        
        Integer stockQuantity = stockUpdate.get("stockQuantity");
        if (stockQuantity == null) {
            return ResponseEntity.badRequest().build();
        }
        
        ProductDTO updatedProduct = productService.updateProductStock(id, stockQuantity);
        return ResponseEntity.ok(updatedProduct);
    }
}