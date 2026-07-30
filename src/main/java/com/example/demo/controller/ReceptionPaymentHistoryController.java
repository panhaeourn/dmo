package com.example.demo.controller;

import com.example.demo.entity.CitoReceipt;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.CitoReceiptRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reception/payment-history")
@RequiredArgsConstructor
public class ReceptionPaymentHistoryController {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final CitoReceiptRepository receiptRepository;

    @GetMapping
    @Transactional
    public synchronized List<PaymentHistory> getMyReceiptPayments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        repairLegacyPaidReceipts(authentication.getName());
        return paymentHistoryRepository.findReceiptPaymentsByReceptionistEmail(
                authentication.getName()
        );
    }

    private void repairLegacyPaidReceipts(String email) {
        List<CitoReceipt> receipts =
                receiptRepository.findByCreatedByReceptionistIgnoreCaseOrderByIdDesc(email);

        for (CitoReceipt receipt : receipts) {
            if (!"Paid".equalsIgnoreCase(receipt.getPaymentStatus())) {
                continue;
            }

            PaymentHistory history = paymentHistoryRepository
                    .findFirstByReceiptIdOrderByIdDesc(receipt.getId())
                    .orElseGet(PaymentHistory::new);
            if ("PAID".equalsIgnoreCase(history.getStatus()) && history.getPaidAt() != null) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            history.setPaymentType("RECEIPT");
            history.setReceiptId(receipt.getId());
            history.setCourseId(null);
            history.setStudentId(receipt.getStudentId());
            history.setStudentName(receipt.getStudentName());
            history.setCourseName(receipt.getCourseName());
            history.setAmount(receipt.getTotalPrice());
            history.setPaymentMethod("BAKONG");
            history.setTransactionRef(receipt.getStudentId());
            history.setBakongMd5(receipt.getBakongTranId());
            history.setStatus("PAID");
            history.setUpdatedAt(now);
            history.setPaidAt(now);
            history.setCheckedBy(email);
            history.setNote("Legacy paid receipt history repaired");
            if (history.getCreatedAt() == null) {
                history.setCreatedAt(now);
            }
            paymentHistoryRepository.save(history);
        }
    }
}
