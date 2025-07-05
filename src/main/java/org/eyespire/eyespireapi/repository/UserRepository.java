package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    Page<User> findByRole(UserRole role, Pageable pageable);
    List<User> findByRoleOrderByNameAsc(UserRole role);
    List<User> findByRoleIn(List<UserRole> roles);
    
    // Tìm kiếm người dùng theo từ khóa (tên, email, số điện thoại)
    Page<User> findByNameContainingOrEmailContainingOrPhoneContaining(
            String name, String email, String phone, Pageable pageable);
    
    // Lọc người dùng theo trạng thái
    Page<User> findByStatus(String status, Pageable pageable);
    
    // Tìm kiếm người dùng theo từ khóa và vai trò
    Page<User> findByNameContainingAndRole(String name, UserRole role, Pageable pageable);
    
    // Tìm kiếm người dùng theo từ khóa và trạng thái
    Page<User> findByNameContainingAndStatus(String name, String status, Pageable pageable);
    
    // Tìm kiếm người dùng theo vai trò và trạng thái
    Page<User> findByRoleAndStatus(UserRole role, String status, Pageable pageable);
}
