package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.PasswordReset;
import org.eyespire.eyespireapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Integer> {
    Optional<PasswordReset> findByOtpCode(String otpCode);
    Optional<PasswordReset> findByUserEmail(String email);
    void deleteByUser(User user);
}
