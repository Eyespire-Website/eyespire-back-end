package org.eyespire.eyespireapi.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.eyespire.eyespireapi.dto.AddToCartRequest;
import org.eyespire.eyespireapi.dto.CartDTO;
import org.eyespire.eyespireapi.dto.CartItemDTO;
import org.eyespire.eyespireapi.dto.UpdateCartItemRequest;
import org.eyespire.eyespireapi.model.Cart;
import org.eyespire.eyespireapi.model.CartItem;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.repository.CartItemRepository;
import org.eyespire.eyespireapi.repository.CartRepository;
import org.eyespire.eyespireapi.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;

    /**
     * Lấy giỏ hàng của người dùng hiện tại
     * @return CartDTO chứa thông tin giỏ hàng
     */
    public CartDTO getCurrentUserCart() {
        User currentUser = userService.getCurrentUser();
        Cart cart = cartRepository.findByPatient(currentUser)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setPatient(currentUser);
                    return cartRepository.save(newCart);
                });
        
        return convertToDTO(cart);
    }

    /**
     * Thêm sản phẩm vào giỏ hàng
     * @param request Thông tin sản phẩm cần thêm
     * @return CartDTO sau khi đã thêm sản phẩm
     */
    @Transactional
    public CartDTO addItemToCart(AddToCartRequest request) {
        User currentUser = userService.getCurrentUser();
        Cart cart = cartRepository.findByPatient(currentUser)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setPatient(currentUser);
                    return cartRepository.save(newCart);
                });

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        // Cập nhật số lượng
        cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        cartItemRepository.save(cartItem);

        return getCurrentUserCart();
    }

    /**
     * Thêm sản phẩm vào giỏ hàng bằng userId
     * @param userId ID của người dùng
     * @param request Thông tin sản phẩm cần thêm
     * @return CartDTO sau khi đã thêm sản phẩm
     */
    @Transactional
    public CartDTO addItemToCartByUserId(Long userId, AddToCartRequest request) {
        User user = userService.getUserById(userId.intValue());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        
        Cart cart = cartRepository.findByPatient(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setPatient(user);
                    return cartRepository.save(newCart);
                });

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProduct(product);
                    newItem.setQuantity(0);
                    return newItem;
                });

        // Cập nhật số lượng
        cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        cartItemRepository.save(cartItem);

        return getUserCartById(userId);
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng
     * @param cartItemId ID của item trong giỏ hàng
     * @param request Thông tin cập nhật
     * @return CartDTO sau khi đã cập nhật
     */
    @Transactional
    public CartDTO updateCartItem(Integer cartItemId, UpdateCartItemRequest request) {
        User currentUser = userService.getCurrentUser();
        Cart cart = cartRepository.findByPatient(currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        // Kiểm tra xem item có thuộc giỏ hàng của người dùng hiện tại không
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to update this cart item");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return getCurrentUserCart();
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng
     * @param cartItemId ID của item trong giỏ hàng
     * @return CartDTO sau khi đã xóa sản phẩm
     */
    @Transactional
    public CartDTO removeCartItem(Integer cartItemId) {
        User currentUser = userService.getCurrentUser();
        Cart cart = cartRepository.findByPatient(currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        // Kiểm tra xem item có thuộc giỏ hàng của người dùng hiện tại không
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to remove this cart item");
        }

        cartItemRepository.delete(cartItem);

        return getCurrentUserCart();
    }

    /**
     * Xóa toàn bộ giỏ hàng
     * @return CartDTO rỗng
     */
    @Transactional
    public CartDTO clearCart() {
        User currentUser = userService.getCurrentUser();
        Cart cart = cartRepository.findByPatient(currentUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        cartItemRepository.deleteByCart(cart);

        return getCurrentUserCart();
    }

    /**
     * Xóa toàn bộ giỏ hàng của một người dùng cụ thể theo ID
     * @param userId ID của người dùng
     * @return CartDTO rỗng
     */
    @Transactional
    public CartDTO clearCartByUserId(Integer userId) {
        System.out.println("[DEBUG] clearCartByUserId called with userId: " + userId);
        
        User user = userService.getUserById(userId);
        if (user == null) {
            System.out.println("[DEBUG] User not found with ID: " + userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        System.out.println("[DEBUG] User found: " + user.getName());
        
        Cart cart = cartRepository.findByPatient(user)
                .orElseThrow(() -> {
                    System.out.println("[DEBUG] Cart not found for user: " + userId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found");
                });
        System.out.println("[DEBUG] Cart found with ID: " + cart.getId());
        
        // Get cart items count before deletion
        List<CartItem> itemsToDelete = cartItemRepository.findByCart(cart);
        System.out.println("[DEBUG] Found " + itemsToDelete.size() + " items to delete");
        
        System.out.println("[DEBUG] About to delete all cart items for cart ID: " + cart.getId());
        cartItemRepository.deleteByCart(cart);
        System.out.println("[DEBUG] All cart items deleted successfully");
        
        // Force commit the transaction
        cartItemRepository.flush();
        System.out.println("[DEBUG] Repository flushed - changes committed to database");

        CartDTO result = getUserCartById(Long.valueOf(userId));
        System.out.println("[DEBUG] Returning cart with " + result.getTotalItems() + " items");
        return result;
    }

    /**
     * Đồng bộ giỏ hàng từ localStorage lên server
     * @param items Danh sách sản phẩm từ localStorage
     * @return CartDTO sau khi đã đồng bộ
     */
    @Transactional
    public CartDTO syncCartFromLocalStorage(List<AddToCartRequest> items) {
        // Xóa giỏ hàng hiện tại
        clearCart();
        
        // Thêm từng sản phẩm từ localStorage vào giỏ hàng
        for (AddToCartRequest item : items) {
            addItemToCart(item);
        }
        
        return getCurrentUserCart();
    }
    
    private CartDTO convertToDTO(Cart cart) {
        CartDTO cartDTO = new CartDTO();
        cartDTO.setId(cart.getId());
        cartDTO.setUserId(cart.getPatient().getId());
        cartDTO.setUserName(cart.getPatient().getName());
        
        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        cartDTO.setItems(itemDTOs);
        
        // Tính tổng số lượng và tổng giá
        int totalItems = itemDTOs.stream().mapToInt(CartItemDTO::getQuantity).sum();
        double totalPrice = itemDTOs.stream().mapToDouble(CartItemDTO::getTotalPrice).sum();
        
        cartDTO.setTotalItems(totalItems);
        cartDTO.setTotalPrice(totalPrice);
        
        return cartDTO;
    }
    
    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setProductImage(cartItem.getProduct().getImageUrl());
        dto.setQuantity(cartItem.getQuantity());
        dto.setPrice(cartItem.getProduct().getPrice().doubleValue());
        dto.setTotalPrice(cartItem.getQuantity() * cartItem.getProduct().getPrice().doubleValue());
        return dto;
    }

    /**
     * Cập nhật số lượng sản phẩm trong giỏ hàng bằng userId
     * @param cartItemId ID của item trong giỏ hàng
     * @param request Thông tin cập nhật
     * @param userId ID của người dùng
     * @return CartDTO sau khi đã cập nhật
     */
    @Transactional
    public CartDTO updateCartItemByUserId(Integer cartItemId, UpdateCartItemRequest request, Integer userId) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        
        Cart cart = cartRepository.findByPatient(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));

        // Kiểm tra xem item có thuộc giỏ hàng của người dùng không
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to update this cart item");
        }

        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return getUserCartById(Long.valueOf(userId));
    }

    /**
     * Xóa một sản phẩm khỏi giỏ hàng bằng userId
     * @param cartItemId ID của item trong giỏ hàng
     * @param userId ID của người dùng
     * @return CartDTO sau khi đã xóa sản phẩm
     */
    @Transactional
    public CartDTO removeCartItemByUserId(Integer cartItemId, Integer userId) {
        System.out.println("[DEBUG] removeCartItemByUserId called with cartItemId: " + cartItemId + ", userId: " + userId);
        
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        System.out.println("[DEBUG] User found: " + user.getName());
        
        Cart cart = cartRepository.findByPatient(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        System.out.println("[DEBUG] Cart found with ID: " + cart.getId());

        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        System.out.println("[DEBUG] CartItem found - ID: " + cartItem.getId() + ", Product: " + cartItem.getProduct().getName() + ", Cart ID: " + cartItem.getCart().getId());

        // Kiểm tra xem item có thuộc giỏ hàng của người dùng không
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            System.out.println("[DEBUG] Permission denied - CartItem cart ID: " + cartItem.getCart().getId() + ", User cart ID: " + cart.getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to remove this cart item");
        }
        
        System.out.println("[DEBUG] About to delete cart item with ID: " + cartItem.getId());
        cartItemRepository.delete(cartItem);
        System.out.println("[DEBUG] Cart item deleted successfully");
        
        // Force flush to ensure deletion is committed
        cartItemRepository.flush();
        System.out.println("[DEBUG] Repository flushed");

        CartDTO result = getUserCartById(Long.valueOf(userId));
        System.out.println("[DEBUG] Returning cart with " + result.getTotalItems() + " items");
        return result;
    }

    /**
     * Lấy giỏ hàng của người dùng theo ID
     * @param userId ID của người dùng
     * @return CartDTO chứa thông tin giỏ hàng
     */
    public CartDTO getUserCartById(Long userId) {
        User user = userService.getUserById(userId.intValue());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        
        Cart cart = cartRepository.findByPatient(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setPatient(user);
                    return cartRepository.save(newCart);
                });
        
        return convertToDTO(cart);
    }
}
