package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Cart;
import org.eyespire.eyespireapi.model.CartItem;
import org.eyespire.eyespireapi.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCart(Cart cart);
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    void deleteByCart(Cart cart);
}
