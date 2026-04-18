package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String paymentType; // RECEIPT or COURSE

    private Long receiptId;
    private Long courseId;

    private String studentId;
    private String studentName;
    private String courseName;

    private Double amount;

    private String paymentMethod;   // BAKONG / ABA / ACLEDA
    private String transactionRef;  // your transaction id / student id / reference
    private String bakongMd5;

    private String status;          // PENDING / PAID / FAILED / EXPIRED

    @Column(columnDefinition = "TEXT")
    private String gatewayResponse;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;

    private String checkedBy;
    private String note;

    public PaymentHistory() {
    }

    public Long getId() {
        return id;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getBakongMd5() {
        return bakongMd5;
    }

    public void setBakongMd5(String bakongMd5) {
        this.bakongMd5 = bakongMd5;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getCheckedBy() {
        return checkedBy;
    }

    public void setCheckedBy(String checkedBy) {
        this.checkedBy = checkedBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}