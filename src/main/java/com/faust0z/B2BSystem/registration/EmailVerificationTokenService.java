package com.faust0z.B2BSystem.registration;

import com.faust0z.B2BSystem.registration.EmailVerificationTokenEntity;
import com.faust0z.B2BSystem.registration.ExpiredRegistrationTokenException;
import com.faust0z.B2BSystem.registration.InvalidRegistrationTokenException;
import com.faust0z.B2BSystem.registration.EmailVerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import com.faust0z.B2BSystem.registration.EmailVerificationTokenService;
import com.faust0z.B2BSystem.registration.EmailVerificationTokenEntity;
import com.faust0z.B2BSystem.registration.ExpiredRegistrationTokenException;
import com.faust0z.B2BSystem.registration.InvalidRegistrationTokenException;
import com.faust0z.B2BSystem.registration.EmailVerificationTokenRepository;
@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;

    @Transactional
    public String issue(UUID userId, Duration ttl) {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        EmailVerificationTokenEntity entity = new EmailVerificationTokenEntity();
        entity.setToken(hashToken(rawToken));
        entity.setUserId(userId);
        entity.setExpiresAt(Instant.now().plus(ttl));
        tokenRepository.save(entity);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public UUID resolve(String rawToken) {
        EmailVerificationTokenEntity entity = tokenRepository.findByToken(hashToken(rawToken))
                .orElseThrow(InvalidRegistrationTokenException::new);

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            tokenRepository.delete(entity);
            throw new ExpiredRegistrationTokenException();
        }

        return entity.getUserId();
    }

    @Transactional
    public void consume(String rawToken) {
        tokenRepository.deleteByToken(hashToken(rawToken));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}