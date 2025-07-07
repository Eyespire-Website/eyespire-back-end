package org.eyespire.eyespireapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.eyespire.eyespireapi.dto.AddToCartRequest;
import org.eyespire.eyespireapi.dto.CartDTO;
import org.eyespire.eyespireapi.dto.UpdateCartItemRequest;
import org.eyespire.eyespireapi.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Lấy giỏ hàng của người dùng hiện tại hoặc người dùng theo ID
     * @param userId ID của người dùng (tùy chọn)
     * @return Thông tin giỏ hàng
     */
    @GetMapping
    public ResponseEntity<CartDTO> getCurrentUserCart(@RequestParam(required = false) Long userId) {
        if (userId != null) {
            return ResponseEntity.ok(cartService.getUserCartById(userId));
        }
        return ResponseEntity.ok(cartService.getCurrentUserCart());
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * @param request Thông tin sản phẩm cần thêm
     * @param userId ID của người dùng (tùy chọn)
     * @return Thông tin giỏ hàng sau khi đã thêm sản phẩm
     */
    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItemToCart(
            @Valid @RequestBody AddToCartRequest request,
            @RequestParam(required = false) Long userId) {
        if (userId != null) {
            return ResponseEntity.ok(cartService.addItemToCartByUserId(userId, request));
        }
        return ResponseEntity.ok(cartService.addItemToCart(request));
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     * @param cartItemId ID của item trong giỏ hàng
     * @param request Thông tin cập nhật
     * @return Thông tin giỏ hàng sau khi đã cập nhật
     */
    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<CartDTO> updateCartItem(
            @PathVariable Integer cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItem(cartItemId, request));
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     * @param cartItemId ID của item trong giỏ hàng
     * @return Thông tin giỏ hàng sau khi đã xóa sản phẩm
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartDTO> removeCartItem(@PathVariable Integer cartItemId) {
        return ResponseEntity.ok(cartService.removeCartItem(cartItemId));
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * @return Thông tin giỏ hàng rỗng
     */
    @DeleteMapping
    public ResponseEntity<CartDTO> clearCart() {
        return ResponseEntity.ok(cartService.clearCart());
    }

    /**
     * Đồng bộ giỏ hàng từ localStorage lên server
     * @param items Danh sách sản phẩm từ localStorage
     * @return Thông tin giỏ hàng sau khi đã đồng bộ
     */
    @PostMapping("/sync")
    public ResponseEntity<CartDTO> syncCartFromLocalStorage(@Valid @RequestBody List<AddToCartRequest> items) {
        return ResponseEntity.ok(cartService.syncCartFromLocalStorage(items));
    }
}
