package com.UngDung.ThucPhamXanh.BotAI;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface OpenAIAPI {
    @POST("v1/chat/completions")
    @Headers({
            "Content-Type: application/json"
    })
    Call<OpenAIResponse> getChatCompletion(
            @Header("Authorization") String authorization,
            @Body OpenAIRequest request
    );

    @POST("v1/chat/completions")
    @Headers({
            "Content-Type: application/json"
    })
    Call<OpenAIResponse> getChatCompletionWithHistory(
            @Header("Authorization") String authorization,
            @Body OpenAIRequestWithHistory request
    );
}