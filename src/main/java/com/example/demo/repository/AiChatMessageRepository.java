package com.example.demo.repository;

import com.example.demo.entity.AiChatMessage;
import com.example.demo.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findTop50ByUserAndCreatedAtAfterOrderByCreatedAtDesc(
            AppUser user,
            LocalDateTime createdAt
    );

    List<AiChatMessage> findTop16ByUserAndCreatedAtAfterOrderByCreatedAtDesc(
            AppUser user,
            LocalDateTime createdAt
    );

    long countByUserAndRoleAndCreatedAtAfter(
            AppUser user,
            String role,
            LocalDateTime createdAt
    );

    @Transactional
    long deleteByCreatedAtBefore(LocalDateTime createdAt);
}
