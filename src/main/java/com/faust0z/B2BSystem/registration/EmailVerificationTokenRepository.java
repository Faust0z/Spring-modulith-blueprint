package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.registration.EmailVerificationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.faust0z.B2BSystem.registration.EmailVerificationTokenEntity;
import com.faust0z.B2BSystem.registration.EmailVerificationTokenRepository;
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationTokenEntity, String> {

    Optional<EmailVerificationTokenEntity> findByToken(String token);

    void deleteByToken(String token);
}