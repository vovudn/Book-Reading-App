package com.example.bookapp.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AlertDialog;

import com.example.bookapp.models.HmacUtil;

import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

import okhttp3.*;

public class MomoPaymentHelper {

    private static final String TAG = "MomoPayment";
    private static final String partnerCode = "MOMO";
    private static final String accessKey = "F8BBA842ECF85";
    private static final String secretKey = "K951B6PE1waDMi640xX08PD3vg6EkVlz";
    private static final String endpoint = "https://test-payment.momo.vn/v2/gateway/api/create";

    public static void startPayment(Context context, int amount, String bookTitle) {
        try {
            String orderId = UUID.randomUUID().toString();
            String requestId = UUID.randomUUID().toString();
            String orderInfo = "Thanh toán sách: " + bookTitle;
            String returnUrl = "https://momo.vn/return";
            String notifyUrl = "https://momo.vn/notify";
            String extraData = "";

            // Tạo chuỗi raw data để ký
            String rawData = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&ipnUrl=" + notifyUrl +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&partnerCode=" + partnerCode +
                    "&redirectUrl=" + returnUrl +
                    "&requestId=" + requestId +
                    "&requestType=captureWallet";

            String signature = HmacUtil.sha256(rawData, secretKey);

            JSONObject json = new JSONObject();
            json.put("partnerCode", partnerCode);
            json.put("accessKey", accessKey);
            json.put("requestId", requestId);
            json.put("amount", String.valueOf(amount));
            json.put("orderId", orderId);
            json.put("orderInfo", orderInfo);
            json.put("redirectUrl", returnUrl);
            json.put("ipnUrl", notifyUrl);
            json.put("extraData", extraData);
            json.put("requestType", "captureWallet");
            json.put("signature", signature);
            json.put("lang", "vi");

            OkHttpClient client = new OkHttpClient();
            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Request failed", e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String resp = response.body().string();
                        try {
                            JSONObject respObj = new JSONObject(resp);
                            String payUrl = respObj.getString("payUrl");

                            // Mở WebView để user thanh toán
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(payUrl));
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "JSON error: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Response failed: " + response.message());
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }
}
