package com.example.demo.controller;

import com.example.demo.entity.CitoReceipt;
import com.example.demo.entity.PaymentHistory;
import com.example.demo.repository.CitoReceiptRepository;
import com.example.demo.repository.PaymentHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceptionPaymentHistoryControllerTest {

    @Test
    void returnsOnlyCurrentReceptionistsReceiptPayments() {
        PaymentHistoryRepository repository = mock(PaymentHistoryRepository.class);
        CitoReceiptRepository receiptRepository = mock(CitoReceiptRepository.class);
        ReceptionPaymentHistoryController controller =
                new ReceptionPaymentHistoryController(repository, receiptRepository);
        PaymentHistory payment = new PaymentHistory();
        var authentication = new UsernamePasswordAuthenticationToken(
                "receptionist@example.com",
                null,
                List.of()
        );

        when(repository.findReceiptPaymentsByReceptionistEmail("receptionist@example.com"))
                .thenReturn(List.of(payment));
        when(receiptRepository.findByCreatedByReceptionistIgnoreCaseOrderByIdDesc(
                "receptionist@example.com"
        )).thenReturn(List.of());

        assertThat(controller.getMyReceiptPayments(authentication)).containsExactly(payment);
        verify(repository).findReceiptPaymentsByReceptionistEmail("receptionist@example.com");
    }

    @Test
    void repairsPaidReceiptWithoutPaidHistory() {
        PaymentHistoryRepository repository = mock(PaymentHistoryRepository.class);
        CitoReceiptRepository receiptRepository = mock(CitoReceiptRepository.class);
        ReceptionPaymentHistoryController controller =
                new ReceptionPaymentHistoryController(repository, receiptRepository);
        CitoReceipt receipt = new CitoReceipt();
        receipt.setId(8L);
        receipt.setPaymentStatus("Paid");
        receipt.setStudentId("CITO001");
        receipt.setStudentName("Student");
        receipt.setCourseName("Course");
        receipt.setTotalPrice(25.0);
        PaymentHistory pending = new PaymentHistory();
        pending.setStatus("PENDING");
        var authentication = new UsernamePasswordAuthenticationToken(
                "receptionist@example.com",
                null,
                List.of()
        );

        when(receiptRepository.findByCreatedByReceptionistIgnoreCaseOrderByIdDesc(
                "receptionist@example.com"
        )).thenReturn(List.of(receipt));
        when(repository.findFirstByReceiptIdOrderByIdDesc(8L))
                .thenReturn(java.util.Optional.of(pending));

        controller.getMyReceiptPayments(authentication);

        assertThat(pending.getStatus()).isEqualTo("PAID");
        assertThat(pending.getPaidAt()).isNotNull();
        assertThat(pending.getCheckedBy()).isEqualTo("receptionist@example.com");
        verify(repository).save(pending);
    }
}
