package com.example.mapbackend.controller;

import com.example.mapbackend.entity.Comment;
import com.example.mapbackend.entity.CommentMedia;
import com.example.mapbackend.entity.User;
import com.example.mapbackend.packets.Packets;
import com.example.mapbackend.repository.CommentMediaRepository;
import com.example.mapbackend.repository.CommentRepository;
import com.example.mapbackend.repository.UserRepository;
import com.example.mapbackend.service.JwtService;
import com.example.mapbackend.service.PasswordService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class CommentController {
    @Value("${map.file_save_path}")
    String fileSavePath;

    @Resource
    UserRepository userRepository;

    @Resource
    CommentRepository commentRepository;

    @Resource
    CommentMediaRepository commentMediaRepository;

    @Resource
    PasswordService passwordService;

    @Resource
    JwtService jwtService;

    @GetMapping("/comment/{siteIdentifier}")
    Packets.GetCommentListResponse getCommentList(@PathVariable String siteIdentifier) {
        List<Packets.CommentInformation> comments = commentRepository.findAllBySiteIdentifier(siteIdentifier)
            .stream().map(comment -> Packets.CommentInformation.builder()
                .comment(comment.getContent())
                .siteIdentifier(comment.getSiteIdentifier())
                .username(comment.getUser().getUsername())
                .imageUrls(comment.getMediaList().stream().map(CommentMedia::getMediaUrl).toList())
                .rating(comment.getRate())
                .build()
            ).toList();

        return Packets.GetCommentListResponse.builder()
            .success(true)
            .message("获取评论列表成功")
            .comments(comments)
            .build();
    }

    @PostMapping(value = "/comment/{siteIdentifier}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Packets.PostMediaToExistingCommentResponse postMediaToExistingComment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String siteIdentifier,
        @RequestParam("file") MultipartFile file
    ) {
        Optional<User> userOpt = jwtService.getUserFromJwtToken(jwt);
        if (userOpt.isEmpty()) {
            return Packets.PostMediaToExistingCommentResponse.builder()
                .success(false)
                .message("用户不存在")
                .build();
        }
        User user = userOpt.get();

        Optional<Comment> commentOpt = commentRepository.findBySiteIdentifierAndUserId(siteIdentifier, user.getId());
        if (commentOpt.isEmpty()) {
            return Packets.PostMediaToExistingCommentResponse.builder()
                .success(false)
                .message("评论不存在")
                .build();
        }
        Comment comment = commentOpt.get();

        if (comment.getUserId() != user.getId()) {
            return Packets.PostMediaToExistingCommentResponse.builder()
                .success(false)
                .message("不可为他人的评论追加图片")
                .build();
        }

        // Create the file.
        Path savingAbsolutePath = Paths.get(fileSavePath).toAbsolutePath().normalize();
        String fileName = file.getOriginalFilename();

        if (fileName == null) {
            return Packets.PostMediaToExistingCommentResponse.builder()
                .success(false)
                .message("文件名为空")
                .build();
        }


        // 重新规定文件名为 上传者用户名+原本的名字
        fileName = user.getUsername() + fileName;

        if (!savingAbsolutePath.toFile().exists()) {
            try {
                Files.createDirectories(savingAbsolutePath);
            }
            catch (Exception ignored) {}
        }
        savingAbsolutePath = savingAbsolutePath.resolve(fileName);

        // Write to the file
        try {
            file.transferTo(savingAbsolutePath);
        }
        catch (Exception e) {
            return Packets.PostMediaToExistingCommentResponse.builder()
                .success(false)
                .message("文件转移过程中出现错误: " + e.getMessage())
                .build();
        }

        CommentMedia media = CommentMedia.builder()
            .commentId(comment.getId())
            .userId(user.getId())
            .mediaUrl(fileName)
            .build();
        commentMediaRepository.save(media);

        return Packets.PostMediaToExistingCommentResponse.builder()
            .success(true)
            .message("图片上传完成")
            .build();
    }

    @PostMapping("/comment")
    Packets.PostCommentResponse postComment(
        @AuthenticationPrincipal Jwt jwt,
        @RequestBody Packets.PostCommentPacket request
    ) {
        Optional<User> userOpt = jwtService.getUserFromJwtToken(jwt);
        if (userOpt.isEmpty()) {
            return Packets.PostCommentResponse.builder()
                .success(false)
                .message("用户不存在")
                .build();
        }
        User user = userOpt.get();

        Comment comment = Comment.builder()
            .content(request.getComment())
            .userId(user.getId())
            .siteIdentifier(request.getSiteIdentifier())
            .mediaList(new ArrayList<>())
            .rate(request.getRating())
            .build();

        try {
            commentRepository.save(comment);
        }
        catch (Exception e) {
            return Packets.PostCommentResponse.builder()
                .success(false)
                .message("评论失败，每人每个站点限评论一次")
                .build();
        }

        return Packets.PostCommentResponse.builder()
            .success(true)
            .message("评论成功")
            .build();
    }

    @DeleteMapping("/comment/{siteIdentifier}")
    Packets.DeleteCommentResponse deleteComment(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String siteIdentifier
    ) {
        Optional<User> userOpt = jwtService.getUserFromJwtToken(jwt);
        if (userOpt.isEmpty()) {
            return Packets.DeleteCommentResponse.builder()
                .success(false)
                .message("删除评论失败：操作用户不存在")
                .build();
        }
        User user = userOpt.get();
        Optional<Comment> commentOpt = commentRepository.findBySiteIdentifierAndUserId(siteIdentifier, user.getId());
        if (commentOpt.isEmpty()) {
            return Packets.DeleteCommentResponse.builder()
                .success(false)
                .message("删除评论失败：评论不存在")
                .build();
        }
        Comment comment = commentOpt.get();

        if (comment.getUserId() != user.getId()) {
            return Packets.DeleteCommentResponse.builder()
                .success(false)
                .message("删除评论失败：不可删除他人评论")
                .build();
        }

        try {
            commentMediaRepository.deleteAll(comment.getMediaList());
            commentRepository.delete(comment);
        }
        catch (Exception e) {
            return Packets.DeleteCommentResponse.builder()
                .success(false)
                .message("删除评论失败: " + e.getMessage())
                .build();
        }
        return Packets.DeleteCommentResponse.builder()
            .success(true)
            .message("删除评论成功")
            .build();
    }
}
