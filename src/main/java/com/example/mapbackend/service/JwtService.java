package com.example.mapbackend.service;

import com.example.mapbackend.entity.User;
import com.example.mapbackend.repository.UserRepository;
import jakarta.annotation.Resource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class JwtService {
    @Resource
    UserRepository userRepository;

    @Resource
    JwtEncoder jwtEncoder;

    public Optional<User> getUserFromJwtToken(Jwt token) {
        if (token == null) {
            return Optional.empty();
        }

        String username = token.getClaimAsString("username");
        return userRepository.findByUsername(username);
    }

    public String createSignedJwtToken(
            String subject,
            String username
    ) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(3600)
                .plusSeconds(360000000);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("GG")
                .issuedAt(now)
                .expiresAt(expireAt)
                .subject(subject)
                .claim("username", username)
                .build();
        return jwtEncoder.encode(
                JwtEncoderParameters.from(claims)
        ).getTokenValue();
    }
}
