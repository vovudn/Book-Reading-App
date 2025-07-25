
package com.example.bookapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.bookapp.MyApplication;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import org.json.JSONObject;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ZaloPaymentResultActivity extends Activity {

    private static final String TAG = "ZaloPaymentResult";

    // Số lượt tải tối đa sau khi thanh toán
    private static final int PAID_DOWNLOAD_LIMIT = 3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "ZaloPaymentResultActivity started");
        Toast.makeText(this, "Callback arrived", Toast.LENGTH_SHORT).show();

        Uri uri = getIntent().getData();
        if (uri == null) {
            Log.e(TAG, "No data from intent");
            finish();
            return;
        }

        try {
            String returnCode = uri.getQueryParameter("returncode"); // "1" là thành công
            String appTransId = uri.getQueryParameter("apptransid");
            String zpTransId  = uri.getQueryParameter("zptransid");
            String embedDataStr = uri.getQueryParameter("embeddata");

            Log.d(TAG, "returncode=" + returnCode);
            Log.d(TAG, "apptransid=" + appTransId);
            Log.d(TAG, "zptransid=" + zpTransId);
            Log.d(TAG, "embeddata(raw)=" + embedDataStr);

            if (embedDataStr != null) {
                // Giải mã URL-encoded nếu cần
                embedDataStr = URLDecoder.decode(embedDataStr, StandardCharsets.UTF_8.name());
                Log.d(TAG, "embeddata(decoded)=" + embedDataStr);
            }

            if (!"1".equals(returnCode)) {
                Toast.makeText(this, "Thanh toán thất bại hoặc bị huỷ!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            final String bookId;
            if (embedDataStr != null) {
                JSONObject embedData = new JSONObject(embedDataStr);
                bookId = embedData.optString("bookId", null);
            } else {
                bookId = null;
            }

            if (bookId == null) {
                Toast.makeText(this, "Không lấy được bookId từ callback!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .child("Downloads")
                    .child(bookId);

            Map<String, Object> updates = new HashMap<>();
            updates.put("isPaid", true);
            updates.put("remainingDownloads", PAID_DOWNLOAD_LIMIT);
            updates.put("paidAt", ServerValue.TIMESTAMP);
            if (appTransId != null) updates.put("appTransId", appTransId);
            if (zpTransId != null)  updates.put("zpTransId", zpTransId);

            ref.updateChildren(updates)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();

                        // Lấy thông tin sách để tải
                        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference("Books").child(bookId);
                        bookRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                String bookTitle = snapshot.child("title").getValue(String.class);
                                String bookUrl = snapshot.child("url").getValue(String.class);

                                // Tải sách (nếu bạn đã implement trong MyApplication)
                                MyApplication.downloadBook(ZaloPaymentResultActivity.this, bookId, bookTitle, bookUrl);

                                // Chuyển về PdfDetailActivity, gắn flag để update UI thanh toán thành công
                                Intent intent = new Intent(ZaloPaymentResultActivity.this, PdfDetailActivity.class);
                                intent.putExtra("bookId", bookId);
                                intent.putExtra("payment_success", true);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                Log.e(TAG, "Lỗi lấy dữ liệu sách để tải");
                                // Có thể thêm xử lý lỗi
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Lỗi cập nhật thanh toán", e);
                        Toast.makeText(this, "Lỗi cập nhật thanh toán!", Toast.LENGTH_SHORT).show();
                        finish();
                    });

        } catch (Exception e) {
            Log.e(TAG, "Lỗi xử lý callback", e);
            Toast.makeText(this, "Lỗi xử lý callback!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
