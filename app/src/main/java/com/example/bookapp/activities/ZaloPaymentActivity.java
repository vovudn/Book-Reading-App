package com.example.bookapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.MyApplication;
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

    // UI
    private Button zaloPayBtn, btnFakeCallback;
    private TextView statusTv, titleTv, descriptionTv, categoryTv, priceTv;
    private ImageButton backBtn;

    private String bookId, bookTitle, bookUrl;
    private String currentUid;

    private static final String TAG = "ZaloPayment";

    // Biến thủ công để giả debug mode (true để hiển thị nút fake, false thì không)
    private static final boolean IS_DEBUG = true;
    private boolean justPaid = false;


    // Biến đánh dấu thanh toán thành công để tránh hiện toast 2 lần
    private boolean paymentSuccessHandled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zalo_payment);

        initViews();

        bookId = getIntent().getStringExtra("bookId");
        bookTitle = getIntent().getStringExtra("bookTitle");
        bookUrl = getIntent().getStringExtra("bookUrl");
        currentUid = FirebaseAuth.getInstance().getUid();

        loadBookInfo();
        checkPaymentStatus();

        zaloPayBtn.setOnClickListener(v -> createOrderZaloPay());

//        if (IS_DEBUG) {
//            btnFakeCallback.setOnClickListener(v -> fakePaymentCallback());
//            btnFakeCallback.setVisibility(View.VISIBLE);
//        } else {
//            btnFakeCallback.setVisibility(View.GONE);
//        }

        backBtn.setOnClickListener(v -> finish());
    }

    private void initViews() {
        titleTv = findViewById(R.id.titleTv);
        descriptionTv = findViewById(R.id.descriptionTv);
        categoryTv = findViewById(R.id.categoryTv);
        priceTv = findViewById(R.id.priceTv);
        //statusTv = findViewById(R.id.statusTv);
        zaloPayBtn = findViewById(R.id.zaloPayBtn);
        backBtn = findViewById(R.id.backBtn);
        //btnFakeCallback = findViewById(R.id.btnFakeCallback);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePaymentCallback(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (justPaid) {
            justPaid = false;  // Reset flag
            Intent payIntent = new Intent();
            payIntent.putExtra("payment_success", true);
            // Gọi xử lý callback luôn
            handlePaymentCallback(payIntent);

        }
        checkPaymentStatus();
        handlePaymentCallback(getIntent());
    }

    private void handlePaymentCallback(Intent intent) {
        if (paymentSuccessHandled) return;
        if (intent == null) return;

        boolean success = intent.getBooleanExtra("payment_success", false);
        if (success) {
            paymentSuccessHandled = true;
            Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

            if (bookUrl != null && !bookUrl.isEmpty()) {
                MyApplication.downloadBook(this, bookId, bookTitle, bookUrl);
            } else {
                Toast.makeText(this, "Không có link tải sách để tải.", Toast.LENGTH_SHORT).show();
            }

            checkPaymentStatus();

            intent.removeExtra("payment_success");
        }
    }


//    private void fakePaymentCallback() {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setData(Uri.parse("bookapp_group2_fu://paymentresult?returncode=1&apptransid=xxx&embeddata=%7B%22bookId%22%3A%22" + bookId + "%22%7D"));
//        intent.setPackage(getPackageName());
//        startActivity(intent);
//    }


    private void loadBookInfo() {
        if (bookId == null) {
            Toast.makeText(this, "Book ID không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books").child(bookId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                titleTv.setText(snapshot.child("title").getValue(String.class));
                descriptionTv.setText(snapshot.child("description").getValue(String.class));
                categoryTv.setText(snapshot.child("category").getValue(String.class));
                priceTv.setText("Giá tải: 10.000đ");
            }
            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ZaloPaymentActivity.this, "Lỗi tải sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkPaymentStatus() {
        if (currentUid == null || bookId == null) return;

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
                    statusTv.setText("Đã thanh toán");
                    zaloPayBtn.setText("Đã thanh toán");
                    zaloPayBtn.setEnabled(false);
                } else {
                    statusTv.setText(" Chưa thanh toán");
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
                JSONObject data = orderAPI.createOrder("10000", bookId);

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
        justPaid = true;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(orderUrl));
        startActivity(intent);
    }
}
