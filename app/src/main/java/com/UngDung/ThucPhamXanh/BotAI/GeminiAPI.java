package com.UngDung.ThucPhamXanh.BotAI;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiAPI {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> getChatCompletion(
            @Query("key") String apiKey,
            @Body GeminiRequest request
    );
}