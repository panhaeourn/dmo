package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", unique = true, nullable = false, length = 100)
    private String transactionId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String currency; // USD / KHR

    @Column(nullable = false)
    private String status; // PENDING / PAID / EXPIRED

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(length = 30)
    private String provider; // BAKONG

    @Column(columnDefinition = "TEXT")
    private String qrString;

    @Column(columnDefinition = "TEXT")
    private String qrImage;

    @Column(columnDefinition = "TEXT")
    private String deeplink;

    @Column(columnDefinition = "TEXT")
    private String checkoutUrl;

    @Column(name = "bakong_md5", length = 100)
    private String bakongMd5;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;
}