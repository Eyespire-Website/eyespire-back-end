package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    List<User> findByRoleOrderByNameAsc(UserRole role);
    List<User> findByRoleIn(List<UserRole> roles);
}
