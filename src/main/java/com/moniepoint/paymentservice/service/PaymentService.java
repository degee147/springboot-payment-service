package com.moniepoint.paymentservice.service;

import com.moniepoint.paymentservice.dto.PaymentRequest;
import com.moniepoint.paymentservice.dto.PaymentResponse;
import com.moniepoint.paymentservice.entity.Payment;
import com.moniepoint.paymentservice.entity.PaymentStatus;
import com.moniepoint.paymentservice.exception.DuplicatePaymentException;
import com.moniepoint.paymentservice.exception.PaymentNotFoundException;
import com.moniepoint.paymentservice.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {

        log.info("Processing payment with idempotency key={}", request.getIdempotencyKey());

        Optional<Payment> existing =
                paymentRepository.findByIdempotencyKey(request.getIdempotencyKey());

        if (existing.isPresent()) {
            throw new DuplicatePaymentException("Payment already processed");
        }

        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setDescription(request.getDescription());
        payment.setMerchantId(request.getMerchantId());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setTransactionReference(generateTransactionReference());

        Payment saved = paymentRepository.save(payment);

        processPaymentLogic(saved);

        return mapToResponse(saved);
    }

    private void processPaymentLogic(Payment payment) {
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
    }

    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::mapToResponse);
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .transactionReference(payment.getTransactionReference())
                .createdAt(payment.getCreatedAt())
                .message("Payment processed successfully")
                .build();
    }
}
