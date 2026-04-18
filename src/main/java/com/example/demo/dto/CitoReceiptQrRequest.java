package com.example.demo.dto;

public class CitoReceiptQrRequest {
    private String receiptId;
    private String studentName;
    private String courseName;
    private Double amount;

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
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
}