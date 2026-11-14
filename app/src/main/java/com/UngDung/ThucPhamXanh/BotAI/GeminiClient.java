package com.UngDung.ThucPhamXanh.BotAI;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.UngDung.ThucPhamXanh.BuildConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Gọi Gemini một lần, dùng chung cho các tính năng AI của admin (hỏi đáp số liệu, nhận xét
 * thống kê, viết mô tả sản phẩm). Callback luôn chạy trên luồng giao diện.
 *
 * Khóa API đọc từ BuildConfig.GEMINI_API_KEY (local.properties). Lưu ý: khóa nằm trong APK nên
 * ai lấy được APK đều trích được khóa — nên giới hạn khóa theo package + SHA-1 trên Google Cloud
 * và đặt hạn mức.
 */
public final class GeminiClient {

    private static final String TAG = "GeminiClient";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/";

    public interface Result {
        void onSuccess(String text);
        void onError(String message);
    }

    private static GeminiAPI api;

    private GeminiClient() { }

    private static synchronized GeminiAPI api() {
        if (api == null) {
            OkHttpClient http = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS) // Gemini có thể "suy nghĩ" khá lâu
                    .build();
            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GeminiAPI.class);
        }
        return api;
    }

    public static boolean hasKey() {
        return BuildConfig.GEMINI_API_KEY != null && !BuildConfig.GEMINI_API_KEY.trim().isEmpty();
    }

    /**
     * @param system  chỉ dẫn hệ thống (vai trò + dữ liệu)
     * @param history các lượt hội thoại trước, mỗi phần tử {role, text} với role "user" hoặc "assistant"; có thể null
     * @param user    câu hỏi hiện tại
     */
    public static void ask(@NonNull String system, @Nullable List<String[]> history,
                           @NonNull String user, @NonNull Result callback) {
        if (!hasKey()) {
            callback.onError("Chưa cấu hình GEMINI_API_KEY trong local.properties.");
            return;
        }
        GeminiRequest request = new GeminiRequest(system);
        if (history != null) {
            for (String[] h : history) request.addContent(h[0], h[1]);
        }
        request.addContent("user", user);

        api().getChatCompletion(BuildConfig.GEMINI_API_KEY, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeminiResponse> call, @NonNull Response<GeminiResponse> r) {
                if (r.isSuccessful() && r.body() != null) {
                    String text = r.body().getResponseText();
                    callback.onSuccess(text == null ? "" : text.trim());
                    return;
                }
                Log.e(TAG, "HTTP " + r.code());
                callback.onError(describe(r.code()));
            }

            @Override
            public void onFailure(@NonNull Call<GeminiResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Gọi Gemini thất bại", t);
                callback.onError("Không kết nối được tới AI. Kiểm tra Internet rồi thử lại.");
            }
        });
    }

    static String describe(int code) {
        switch (code) {
            case 400: return "AI không hiểu yêu cầu (400).";
            case 401:
            case 403: return "Khóa AI không hợp lệ hoặc không có quyền (" + code + ").";
            case 429: return "AI đang quá tải hoặc hết hạn mức miễn phí (429). Thử lại sau ít phút.";
            default:  return "Lỗi từ máy chủ AI (" + code + "). Thử lại sau.";
        }
    }
}
