package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByEmail(String email);
    void deleteByEmail(String email);

    Optional<OtpCode> findByEmailAndCode(String email, String code);

}