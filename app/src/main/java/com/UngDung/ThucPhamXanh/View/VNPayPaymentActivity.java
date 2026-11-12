package com.UngDung.ThucPhamXanh.View;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.UngDung.ThucPhamXanh.R;

import java.util.HashMap;
import java.util.Map;

public class VNPayPaymentActivity extends AppCompatActivity {
    private static final String TAG = "VNPayPaymentActivity";
    private WebView webView;
    private ProgressBar progressBar;
    private String paymentUrl;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vnpay_payment);

        // Xử lý deep link từ VNPay return
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null && "vnpay".equals(data.getScheme())) {
            // Đây là deep link từ VNPay return
            handleVNPayReturn(data.toString());
            return;
        }
        
        // Xử lý payment URL từ CartActivity
        paymentUrl = intent.getStringExtra("payment_url");
        orderId = intent.getStringExtra("order_id");

        if (paymentUrl == null || paymentUrl.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không có URL thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadPaymentUrl();
    }

    private void initViews() {
        webView = findViewById(R.id.webview_vnpay);
        progressBar = findViewById(R.id.progress_bar_vnpay);
        
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar_vnpay);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        
        setupWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        
        // Cần thiết để hiển thị nội dung Mixed Content (http trong https) trên webview của một số cổng thanh toán
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        
        // Cần thiết để hiển thị một số nội dung hoặc alert từ VNPay
        webView.setWebChromeClient(new android.webkit.WebChromeClient());
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Loading URL: " + url);
                
                // Xử lý return URL từ VNPay
                if (url.startsWith("vnpay://return") || url.contains("vnp_ResponseCode")) {
                    handleVNPayReturn(url);
                    return true;
                }
                
                // Nếu VNPay chuyển hướng qua intent ngân hàng (ví dụ momo, zalo, vnpay wallet)
                if (url.startsWith("intent://") || url.startsWith("momo://") || url.startsWith("vnpayqr://")) {
                     try {
                         Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                         if (intent != null) {
                             try {
                                 startActivity(intent);
                             } catch (android.content.ActivityNotFoundException e) {
                                 // Nếu chưa cài app, thử dùng fallback URL (thường VNPay sẽ truyền theo)
                                 String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                                 if (fallbackUrl != null) {
                                     view.loadUrl(fallbackUrl);
                                 } else {
                                     Toast.makeText(VNPayPaymentActivity.this, "Bạn chưa cài đặt ứng dụng thanh toán này", Toast.LENGTH_SHORT).show();
                                 }
                             }
                             return true;
                         }
                     } catch (Exception e) {
                         Log.e(TAG, "Error parsing intent URL: " + e.getMessage());
                     }
                }
                
                // Trả về false đối với http/https để WebView tự load
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false;
                }
                
                // Các scheme khác (chưa xử lý được), block để tránh lỗi ERR_UNKNOWN_URL_SCHEME
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                // Bỏ qua lỗi SSL (cần thiết cho một số máy test với VNPay Sandbox)
                handler.proceed();
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void loadPaymentUrl() {
        Log.d(TAG, "Loading payment URL: " + paymentUrl);
        webView.loadUrl(paymentUrl);
    }

    private void handleVNPayReturn(String returnUrl) {
        Log.d(TAG, "Handling VNPay return: " + returnUrl);
        
        try {
            Uri uri = Uri.parse(returnUrl);
            Map<String, String> params = new HashMap<>();
            
            // Parse query parameters
            for (String paramName : uri.getQueryParameterNames()) {
                String paramValue = uri.getQueryParameter(paramName);
                if (paramValue != null) {
                    params.put(paramName, paramValue);
                }
            }
            
            // Get response code
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");
            String amount = params.get("vnp_Amount");
            
            Log.d(TAG, "Response Code: " + responseCode);
            Log.d(TAG, "Transaction No: " + transactionNo);
            Log.d(TAG, "Amount: " + amount);
            
            // Verify signature (optional but recommended)
            // boolean isValid = VNPayHelper.verifyReturnUrl(params);
            // if (!isValid) {
            //     Toast.makeText(this, "Lỗi xác thực chữ ký", Toast.LENGTH_SHORT).show();
            //     setResult(RESULT_CANCELED);
            //     finish();
            //     return;
            // }
            
            // Check payment status
            Intent resultIntent = new Intent();
            resultIntent.putExtra("order_id", orderId);
            resultIntent.putExtra("response_code", responseCode);
            resultIntent.putExtra("transaction_no", transactionNo);
            resultIntent.putExtra("amount", amount);
            
            if ("00".equals(responseCode)) {
                // Payment successful
                resultIntent.putExtra("status", "success");
                resultIntent.putExtra("message", "Thanh toán thành công");
                setResult(RESULT_OK, resultIntent);
                Toast.makeText(this, "Thanh toán thành công", Toast.LENGTH_SHORT).show();
            } else {
                // Payment failed
                String message = com.UngDung.ThucPhamXanh.Helper.VNPayHelper.getResponseMessage(responseCode);
                resultIntent.putExtra("status", "failed");
                resultIntent.putExtra("message", message);
                setResult(RESULT_CANCELED, resultIntent);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
            
            finish();
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling return URL: " + e.getMessage());
            Toast.makeText(this, "Lỗi xử lý kết quả thanh toán", Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            // User cancelled payment
            setResult(RESULT_CANCELED);
            super.onBackPressed();
        }
    }
}

