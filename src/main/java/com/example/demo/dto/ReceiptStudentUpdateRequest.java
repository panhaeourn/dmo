package com.example.demo.dto;

public class ReceiptStudentUpdateRequest {
    private String studentName;
    private String studentNameEnglish;
    private String studentNameKhmer;
    private String gender;
    private String phone;
    private String contactInfo;
    private String email;
    private String address;

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentNameEnglish() { return studentNameEnglish; }
    public void setStudentNameEnglish(String studentNameEnglish) { this.studentNameEnglish = studentNameEnglish; }
    public String getStudentNameKhmer() { return studentNameKhmer; }
    public void setStudentNameKhmer(String studentNameKhmer) { this.studentNameKhmer = studentNameKhmer; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
