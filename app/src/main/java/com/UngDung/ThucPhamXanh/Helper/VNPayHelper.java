package com.UngDung.ThucPhamXanh.Helper;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * VNPay Helper để tạo payment URL
 * ⚠️ LƯU Ý: Trong production, nên tạo payment URL ở backend để bảo mật hash secret
 */
public class VNPayHelper {
    private static final String TAG = "VNPayHelper";
    
    // VNPay Configuration - Thay đổi theo thông tin từ VNPay
    private static final String VNPAY_TMN_CODE = "CL4X9MJI";
    private static final String VNPAY_HASH_SECRET = "FCZFDWOZXCJJRRGSKZURIVLELVVBLXAQ";
    private static final String VNPAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String VNPAY_RETURN_URL = "vnpay://return"; // Deep link cho Android app
    
    /**
     * Tạo VNPay payment URL
     * @param amount Số tiền (VND)
     * @param orderId Mã đơn hàng
     * @param orderInfo Thông tin đơn hàng
     * @param bankCode Mã ngân hàng (optional, null nếu không chọn ngân hàng cụ thể)
     * @return Payment URL
     */
    public static String createPaymentUrl(long amount, String orderId, String orderInfo, String bankCode) {
        try {
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", "2.1.0");
            vnp_Params.put("vnp_Command", "pay");
            vnp_Params.put("vnp_TmnCode", VNPAY_TMN_CODE);
            vnp_Params.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu amount * 100
            vnp_Params.put("vnp_CurrCode", "VND");
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", "billpayment");
            vnp_Params.put("vnp_Locale", "vn");
            vnp_Params.put("vnp_ReturnUrl", VNPAY_RETURN_URL);
            vnp_Params.put("vnp_IpAddr", "127.0.0.1");
            
            // Format date: YYYYMMDDHHmmss
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            
            Date now = new Date();
            String createDate = formatter.format(now);
            vnp_Params.put("vnp_CreateDate", createDate);
            
            // vnp_ExpireDate: CreateDate + 15 minutes (bắt buộc từ bản cập nhật mới của VNPay)
            java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            calendar.setTime(now);
            calendar.add(java.util.Calendar.MINUTE, 15);
            String expireDate = formatter.format(calendar.getTime());
            vnp_Params.put("vnp_ExpireDate", expireDate);
            
            // Chỉ thêm bankCode nếu là mã ngân hàng hợp lệ (không phải payment method)
            if (bankCode != null && !bankCode.trim().isEmpty()) {
                String[] paymentMethods = {"VNPAYQR", "VNBANK", "INTCARD"};
                boolean isPaymentMethod = false;
                for (String method : paymentMethods) {
                    if (method.equalsIgnoreCase(bankCode.trim())) {
                        isPaymentMethod = true;
                        break;
                    }
                }
                
                if (!isPaymentMethod) {
                    vnp_Params.put("vnp_BankCode", bankCode.trim());
                    Log.d(TAG, "Adding bankCode: " + bankCode);
                } else {
                    Log.d(TAG, "Skipping bankCode - this is a payment method: " + bankCode);
                }
            }
            
            // Sort params
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            
            while (itr.hasNext()) {
    String fieldName = itr.next();
    String fieldValue = vnp_Params.get(fieldName);
    if ((fieldValue != null) && (fieldValue.length() > 0)) {
        // Encode bằng tên Charset chuẩn, KHÔNG replace "+"
        hashData.append(fieldName);
        hashData.append('=');
        hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.name()));
        
        // Build query
        query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.name()));
        query.append('=');
        query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.name()));
        
        if (itr.hasNext()) {
            query.append('&');
            hashData.append('&');
        }
    }
}
            
            // Create secure hash
            String queryUrl = query.toString();
            String vnp_SecureHash = hmacSHA512(VNPAY_HASH_SECRET, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            
            String paymentUrl = VNPAY_URL + "?" + queryUrl;
            Log.d(TAG, "Payment URL created: " + paymentUrl);
            return paymentUrl;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment URL: " + e.getMessage());
            return null;
        }
    }
    
    
    /**
     * HMAC SHA512
     */
    private static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] hash = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating HMAC: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verify return URL signature
     */
    public static boolean verifyReturnUrl(Map<String, String> params) {
        try {
            String secureHash = params.get("vnp_SecureHash");
            if (secureHash == null || secureHash.isEmpty()) {
                return false;
            }
            
            // Remove secure hash from params
            Map<String, String> sortedParams = new HashMap<>(params);
            sortedParams.remove("vnp_SecureHash");
            sortedParams.remove("vnp_SecureHashType");
            
            // Sort and create hash data
            List<String> fieldNames = new ArrayList<>(sortedParams.keySet());
            Collections.sort(fieldNames);
            
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = sortedParams.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()).replace("+", "%20"));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }
            
            String calculatedHash = hmacSHA512(VNPAY_HASH_SECRET, hashData.toString());
            return secureHash.equals(calculatedHash);
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying signature: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get response message from response code
     */
    public static String getResponseMessage(String responseCode) {
        if (responseCode == null) return "Lỗi không xác định";
        
        switch (responseCode) {
            case "00":
                return "Giao dịch thành công";
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13":
                return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24":
                return "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51":
                return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            default:
                return "Các lỗi khác (Mã lỗi: " + responseCode + ")";
        }
    }
}

