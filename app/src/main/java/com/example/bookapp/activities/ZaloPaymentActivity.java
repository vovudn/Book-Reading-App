package com.example.bookapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.R;
import com.example.bookapp.models.CreateOrder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.util.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.*;

public class ZaloPaymentActivity extends AppCompatActivity {

    private TextView titleTv, categoryTv, dateTv, sizeTv, viewsTv, downloadsTv, pagesTv, descriptionTv, priceTv, statusTv;
    private Button zaloPayBtn;
    private ImageButton backBtn;

    private String bookId, bookTitle, bookUrl;
    private String currentUid;

    private static final String TAG = "ZaloPayment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zalo_payment);

        titleTv = findViewById(R.id.titleTv);
        categoryTv = findViewById(R.id.categoryTv);
        dateTv = findViewById(R.id.dateTv);
        sizeTv = findViewById(R.id.sizeTv);
        viewsTv = findViewById(R.id.viewsTv);
        downloadsTv = findViewById(R.id.downloadsTv);
        pagesTv = findViewById(R.id.pagesTv);
        descriptionTv = findViewById(R.id.descriptionTv);
        priceTv = findViewById(R.id.priceTv);
        statusTv = findViewById(R.id.statusTv);
        zaloPayBtn = findViewById(R.id.zaloPayBtn);
        backBtn = findViewById(R.id.backBtn);

        bookId = getIntent().getStringExtra("bookId");
        bookTitle = getIntent().getStringExtra("bookTitle");
        bookUrl = getIntent().getStringExtra("bookUrl");
        currentUid = FirebaseAuth.getInstance().getUid();

        loadBookInfo();
        checkPaymentStatus();

        zaloPayBtn.setOnClickListener(v -> createOrderZaloPay());
        backBtn.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Checking payment status...");
        checkPaymentStatus();

        boolean paymentSuccess = getIntent().getBooleanExtra("payment_success", false);
        Log.d(TAG, "onResume: payment_success = " + paymentSuccess);

        if (paymentSuccess) {
            Toast.makeText(this, "🎉 Thanh toán thành công!", Toast.LENGTH_SHORT).show();
            // Gán lại để không hiển thị lại Toast sau
            getIntent().putExtra("payment_success", false);
        }
    }

    private void loadBookInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                titleTv.setText(snapshot.child("title").getValue(String.class));
                descriptionTv.setText(snapshot.child("description").getValue(String.class));
                categoryTv.setText(snapshot.child("category").getValue(String.class));
                priceTv.setText("Giá tải: 10.000đ (2 lượt đầu miễn phí)");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ZaloPaymentActivity.this, "Lỗi tải sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPaymentStatus() {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUid)
                .child("Downloads")
                .child(bookId)
                .child("isPaid");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isPaid = snapshot.getValue(Boolean.class);
                if (isPaid != null && isPaid) {
                    statusTv.setText("✅ Đã thanh toán");
                    zaloPayBtn.setText("Đã thanh toán");
                    zaloPayBtn.setEnabled(false);
                } else {
                    statusTv.setText("❌ Chưa thanh toán");
                    zaloPayBtn.setText("Thanh toán ngay");
                    zaloPayBtn.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                statusTv.setText("Không kiểm tra được thanh toán");
            }
        });
    }

    private void createOrderZaloPay() {
        new Thread(() -> {
            try {
                CreateOrder orderAPI = new CreateOrder();
                JSONObject data = orderAPI.createOrder("10000", bookId); // Giá: 10.000đ

                String orderUrl = data.optString("order_url", null);
                if (orderUrl != null && !orderUrl.isEmpty()) {
                    runOnUiThread(() -> openZaloPay(orderUrl));
                } else {
                    String message = data.optString("return_message", "Không có order_url");
                    runOnUiThread(() ->
                            Toast.makeText(ZaloPaymentActivity.this, "ZaloPay lỗi: " + message, Toast.LENGTH_LONG).show());
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ZaloPaymentActivity.this, "Lỗi tạo đơn hàng: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openZaloPay(String orderUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(orderUrl));
        startActivity(intent);
    }

    // Không cần dùng trong lớp này nữa, để lại nếu dùng custom HMAC
    private String hmacSha256(String data, String key) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
