package com.example.demo.dto;

public class BakongMerchantConfigDto {
    public String bakongAccountId;
    public String merchantName;
    public String merchantCity;
    public String merchantId;
    public String acquiringBank;
    public String currency; // e.g. USD or KHR
    public int qrExpirySeconds;

    public BakongMerchantConfigDto() {}
}
