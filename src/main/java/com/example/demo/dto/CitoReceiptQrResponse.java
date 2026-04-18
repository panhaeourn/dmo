package com.example.demo.dto;

public class CitoReceiptQrResponse {
    private String qrImage;
    private String tranId;

    public CitoReceiptQrResponse() {
    }

    public CitoReceiptQrResponse(String qrImage, String tranId) {
        this.qrImage = qrImage;
        this.tranId = tranId;
    }

    public String getQrImage() {
        return qrImage;
    }

    public void setQrImage(String qrImage) {
        this.qrImage = qrImage;
    }

    public String getTranId() {
        return tranId;
    }

    public void setTranId(String tranId) {
        this.tranId = tranId;
    }
}