package com.example.demo.controller;

import com.example.demo.entity.PaymentHistory;
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
        ReceptionPaymentHistoryController controller =
                new ReceptionPaymentHistoryController(repository);
        PaymentHistory payment = new PaymentHistory();
        var authentication = new UsernamePasswordAuthenticationToken(
                "receptionist@example.com",
                null,
                List.of()
        );

        when(repository.findReceiptPaymentsByReceptionistEmail("receptionist@example.com"))
                .thenReturn(List.of(payment));

        assertThat(controller.getMyReceiptPayments(authentication)).containsExactly(payment);
        verify(repository).findReceiptPaymentsByReceptionistEmail("receptionist@example.com");
    }
}
