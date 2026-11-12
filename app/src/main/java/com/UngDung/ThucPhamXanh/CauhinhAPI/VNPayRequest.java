package com.UngDung.ThucPhamXanh.CauhinhAPI;

public class VNPayRequest {
    private String bookingId;
    private String bankCode;

    public VNPayRequest(String bookingId, String bankCode) {
        this.bookingId = bookingId;
        this.bankCode = bankCode;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }
}

