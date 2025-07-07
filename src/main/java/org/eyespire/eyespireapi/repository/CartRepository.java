package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Cart;
import org.eyespire.eyespireapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByPatient(User patient);
}
