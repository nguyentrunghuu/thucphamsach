package com.UngDung.ThucPhamXanh.CauhinhAPI;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface VNPayApiService {
    @POST("api/payments/vnpay/create")
    Call<VNPayResponse> createPayment(@Body VNPayRequest request);
}

