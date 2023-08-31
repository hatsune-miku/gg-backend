package com.example.mapbackend.controller;

import com.example.mapbackend.entity.User;
import com.example.mapbackend.packets.Packets;
import com.example.mapbackend.service.JwtService;
import jakarta.annotation.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class UserController {
    @Resource
    JwtService jwtService;

    @GetMapping("/user")
    Packets.GetUserInformationResponse getUserInformation(
        @AuthenticationPrincipal Jwt jwt
    ) {
        Optional<User> userOpt = jwtService.getUserFromJwtToken(jwt);
        if (userOpt.isEmpty()) {
            return Packets.GetUserInformationResponse.builder()
                .success(false)
                .message("用户不存在")
                .build();
        }

        return Packets.GetUserInformationResponse.builder()
            .success(true)
            .message("获取用户信息成功")
            .username(userOpt.get().getUsername())
            .build();
    }
}
