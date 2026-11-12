package com.UngDung.ThucPhamXanh.CauhinhAPI;

public class VNPayResponse {
    private boolean success;
    private String message;
    private VNPayData data;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public VNPayData getData() {
        return data;
    }

    public void setData(VNPayData data) {
        this.data = data;
    }

    public static class VNPayData {
        private String paymentUrl;

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public void setPaymentUrl(String paymentUrl) {
            this.paymentUrl = paymentUrl;
        }
    }
}

