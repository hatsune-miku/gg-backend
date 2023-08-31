package com.example.mapbackend.service;

import com.example.mapbackend.util.HashUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PasswordService {
    public boolean isPasswordValid(String plaintextSha256Sha256, String realPasswordSalted) {
        // pass = salt + h(h(h(plaintext)) + salt)
        String saltExtracted = realPasswordSalted.substring(0, 64);
        String passwordRecovered = saltExtracted + HashUtil.sha256(plaintextSha256Sha256 + saltExtracted);
        return passwordRecovered.equals(realPasswordSalted);
    }

    public String createPassword(String hp) {
        // pass = salt + h(h(h(plaintext)) + salt)
        String plaintextSha256Sha256 = HashUtil.sha256(hp);
        String salt = (UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", ""));
        return salt + HashUtil.sha256(plaintextSha256Sha256 + salt);
    }
}
