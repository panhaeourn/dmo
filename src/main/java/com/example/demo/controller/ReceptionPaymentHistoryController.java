package com.example.demo.controller;

import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reception/payment-history")
@RequiredArgsConstructor
public class ReceptionPaymentHistoryController {

    private final PaymentHistoryRepository paymentHistoryRepository;

    @GetMapping
    public List<PaymentHistory> getMyReceiptPayments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        return paymentHistoryRepository.findReceiptPaymentsByReceptionistEmail(
                authentication.getName()
        );
    }
}
