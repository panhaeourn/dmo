package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "cito_receipts")
public class CitoReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String studentId;

    private String studentCode;

    private String receiptType = "COURSE";

    private String courseName;

    private String monthlyPeriod;

    @Column(length = 4000)
    private String monthlyPaidMonths;

    private String studentName;

    private String studentNameEnglish;

    private String studentNameKhmer;

    private String gender;

    private String phone;

    private String contactInfo;

    private String email;

    @Column(length = 1000)
    private String address;

    private String schedule;

    private Double bookPrice;

    private Double programPrice;

    private Double totalPrice;

    private String paymentStatus = "Pending";

    @Column(length = 2000)
    private String qrImage;

    @Column(length = 10000)
    private String qrText;

    private String bakongTranId;

    private String createdAt;

    private String createdByReceptionist;

    private String createdByReceptionistName;

    public CitoReceipt() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getReceiptType() {
        return receiptType;
    }

    public void setReceiptType(String receiptType) {
        this.receiptType = receiptType;
    }

    public String getMonthlyPeriod() {
        return monthlyPeriod;
    }

    public void setMonthlyPeriod(String monthlyPeriod) {
        this.monthlyPeriod = monthlyPeriod;
    }

    public String getMonthlyPaidMonths() {
        return monthlyPaidMonths;
    }

    public void setMonthlyPaidMonths(String monthlyPaidMonths) {
        this.monthlyPaidMonths = monthlyPaidMonths;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentNameEnglish() {
        return studentNameEnglish;
    }

    public void setStudentNameEnglish(String studentNameEnglish) {
        this.studentNameEnglish = studentNameEnglish;
    }

    public String getStudentNameKhmer() {
        return studentNameKhmer;
    }

    public void setStudentNameKhmer(String studentNameKhmer) {
        this.studentNameKhmer = studentNameKhmer;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Double getBookPrice() {
        return bookPrice;
    }

    public void setBookPrice(Double bookPrice) {
        this.bookPrice = bookPrice;
    }

    public Double getProgramPrice() {
        return programPrice;
    }

    public void setProgramPrice(Double programPrice) {
        this.programPrice = programPrice;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getQrImage() {
        return qrImage;
    }

    public void setQrImage(String qrImage) {
        this.qrImage = qrImage;
    }

    public String getQrText() {
        return qrText;
    }

    public void setQrText(String qrText) {
        this.qrText = qrText;
    }

    public String getBakongTranId() {
        return bakongTranId;
    }

    public void setBakongTranId(String bakongTranId) {
        this.bakongTranId = bakongTranId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedByReceptionist() {
        return createdByReceptionist;
    }

    public void setCreatedByReceptionist(String createdByReceptionist) {
        this.createdByReceptionist = createdByReceptionist;
    }

    public String getCreatedByReceptionistName() {
        return createdByReceptionistName;
    }

    public void setCreatedByReceptionistName(String createdByReceptionistName) {
        this.createdByReceptionistName = createdByReceptionistName;
    }
}
