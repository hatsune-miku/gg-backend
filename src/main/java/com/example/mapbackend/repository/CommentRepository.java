package com.example.mapbackend.repository;

import com.example.mapbackend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findAllBySiteIdentifier(String siteIdentifier);
    Optional<Comment> findBySiteIdentifierAndUserId(String siteIdentifier, int userId);
    void deleteBySiteIdentifier(String siteIdentifier);
}
