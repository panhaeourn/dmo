package com.example.demo.controller;

import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.PaymentHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/payment-history")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PaymentHistoryController {

    private final PaymentHistoryRepository paymentHistoryRepository;

    public PaymentHistoryController(PaymentHistoryRepository paymentHistoryRepository) {
        this.paymentHistoryRepository = paymentHistoryRepository;
    }

    @GetMapping
    public List<PaymentHistory> getAll() {
        return paymentHistoryRepository.findAllByOrderByIdDesc();
    }

    @GetMapping("/my")
    public List<PaymentHistory> getMyHistory(Authentication authentication) {
        String studentId = authentication.getName();
        return paymentHistoryRepository.findByStudentIdOrderByIdDesc(studentId);
    }
}