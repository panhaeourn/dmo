package com.example.demo.controller;

import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.PaymentHistoryRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @PatchMapping("/{id}/completion-status")
    public PaymentHistory updateCompletionStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        PaymentHistory row = paymentHistoryRepository.findById(id).orElseThrow();
        String status = body == null ? null : body.get("status");
        if (!"PENDING".equals(status) && !"APPROVED".equals(status)) throw new IllegalArgumentException("Invalid completion status");
        row.setCompletionStatus(status);
        return paymentHistoryRepository.save(row);
    }

    @GetMapping("/my")
    public List<PaymentHistory> getMyHistory(Authentication authentication) {
        String studentId = authentication.getName();
        return paymentHistoryRepository.findByStudentIdOrderByIdDesc(studentId);
    }
}
