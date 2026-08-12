package com.example.demo.repository;

import com.example.demo.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {

    List<PaymentHistory> findAllByOrderByIdDesc();

    Optional<PaymentHistory> findFirstByReceiptIdOrderByIdDesc(Long id);

    List<PaymentHistory> findByReceiptId(Long id);

    Optional<PaymentHistory> findFirstByTransactionRefOrderByIdDesc(String transactionRef);

    List<PaymentHistory> findByStudentIdOrderByIdDesc(String studentId);
    @Query("""
            select payment
            from PaymentHistory payment, CitoReceipt receipt
            where payment.receiptId = receipt.id
              and upper(payment.paymentType) = 'RECEIPT'
              and lower(receipt.createdByReceptionist) = lower(:email)
            order by payment.id desc
            """)
    List<PaymentHistory> findReceiptPaymentsByReceptionistEmail(@Param("email") String email);
}
