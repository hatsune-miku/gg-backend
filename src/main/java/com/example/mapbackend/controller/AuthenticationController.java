package com.example.mapbackend.controller;

import com.example.mapbackend.entity.User;
import com.example.mapbackend.packets.Packets;
import com.example.mapbackend.repository.UserRepository;
import com.example.mapbackend.service.JwtService;
import com.example.mapbackend.service.PasswordService;
import jakarta.annotation.Resource;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class AuthenticationController {
    @Resource
    UserRepository userRepository;

    @Resource
    PasswordService passwordService;

    @Resource
    JwtService jwtService;

    @PostMapping("/auth/token")
    Packets.LoginResponse auth(@RequestBody Packets.LoginPacket request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            return Packets.LoginResponse.builder()
                    .success(false)
                    .message("用户不存在")
                    .build();
        }
        User user = userOpt.get();

        if (!passwordService.isPasswordValid(request.getPassword(), user.getPassword())) {
            return Packets.LoginResponse.builder()
                    .success(false)
                    .message("用户存在，但是密码错误")
                    .build();
        }


        /*
        TODO: 登录加盐，防止重放攻击
        if (!SaltUtil.calculateSaltForLogin(request.getPassword(), user.getUid())
            .equals(request.getSalt()) && !request.getSalt().equals("114514")) {
            return AuthResponse.fail("Your system clock is not up to date, please try again");
        }
         */

        String token = jwtService.createSignedJwtToken("Login Token", user.getUsername());
        return Packets.LoginResponse.builder()
                .success(true)
                .message("登录成功")
                .token(token)
                .build();
    }

    @PostMapping("/auth/sign-up")
    Packets.RegisterResponse signUp(@RequestBody Packets.RegisterPacket request) {
        // hp: h(plaintext)
        String hp = request.getPassword();

        try {
            User user = User.builder()
                .username(request.getUsername())
                .password(passwordService.createPassword(hp))
                .build();

            userRepository.save(user);
            return Packets.RegisterResponse.builder()
                .success(true)
                .message("注册成功")
                .build();
        }
        catch (Exception e) {
            return Packets.RegisterResponse.builder()
                .success(false)
                .message("注册失败: " + e.getMessage())
                .build();
        }
    }
}
