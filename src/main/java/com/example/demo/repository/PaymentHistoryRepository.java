package com.example.demo.repository;

import com.example.demo.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findAllByOrderByIdDesc();

    Optional<PaymentHistory> findFirstByReceiptIdOrderByIdDesc(Long id);

    Optional<PaymentHistory> findFirstByTransactionRefOrderByIdDesc(String transactionRef);

    List<PaymentHistory> findByStudentIdOrderByIdDesc(String studentId);
}