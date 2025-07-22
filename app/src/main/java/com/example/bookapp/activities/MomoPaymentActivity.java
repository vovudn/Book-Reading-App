package com.example.bookapp.activities;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.UUID;

public class MomoPaymentActivity extends AppCompatActivity {

    // Views
    private TextView titleTv, categoryTv, dateTv, sizeTv, viewsTv, downloadsTv, pagesTv, descriptionTv, priceTv, statusTv;
    private Button momoPayBtn;
    private ImageButton backBtn;

    // Data
    private String bookId;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_momo_payment);

        // Init views
        titleTv = findViewById(R.id.titleTv);
        categoryTv = findViewById(R.id.categoryTv);
        dateTv = findViewById(R.id.dateTv);
        sizeTv = findViewById(R.id.sizeTv);
        viewsTv = findViewById(R.id.viewsTv);
        downloadsTv = findViewById(R.id.downloadsTv);
        pagesTv = findViewById(R.id.pagesTv);
        descriptionTv = findViewById(R.id.descriptionTv);
        priceTv = findViewById(R.id.priceTv);           // TextView hiển thị giá
        statusTv = findViewById(R.id.statusTv);         // TextView hiển thị trạng thái đã thanh toán
        momoPayBtn = findViewById(R.id.momoPayBtn);     // Button thanh toán
        backBtn = findViewById(R.id.backBtn);

        // Get bookId from Intent
        bookId = getIntent().getStringExtra("bookId");
        currentUid = FirebaseAuth.getInstance().getUid();

        loadBookInfo();
        checkPaymentStatus();

        // Click thanh toán
//        momoPayBtn.setOnClickListener(v -> {
//            Toast.makeText(this, "Mô phỏng thanh toán MoMo...", Toast.LENGTH_SHORT).show();
//            simulateMomoPayment();
//        });
     /*   momoPayBtn.setOnClickListener(v -> {
            MomoPaymentHelper.startPayment(this, 10000, "Tên Sách XYZ");
        });*/

        momoPayBtn.setOnClickListener(v -> {
            requestPayment();
        });



        backBtn.setOnClickListener(v -> finish());
    }

    private void loadBookInfo() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            titleTv.setText(snapshot.child("title").getValue(String.class));
                            categoryTv.setText(snapshot.child("category").getValue(String.class));

                            Long timestamp = snapshot.child("timestamp").getValue(Long.class);
                            dateTv.setText(timestamp != null ? String.valueOf(timestamp) : ""); // Có thể định dạng ngày nếu cần

                            Long size = snapshot.child("size").getValue(Long.class);
                            sizeTv.setText(size != null ? String.valueOf(size) : "");

                            Long views = snapshot.child("viewsCount").getValue(Long.class);
                            viewsTv.setText(views != null ? String.valueOf(views) : "");

                            Long downloads = snapshot.child("downloadsCount").getValue(Long.class);
                            downloadsTv.setText(downloads != null ? String.valueOf(downloads) : "");

                            Long pages = snapshot.child("pages").getValue(Long.class);
                            pagesTv.setText(pages != null ? String.valueOf(pages) : "");

                            descriptionTv.setText(snapshot.child("description").getValue(String.class));

                            priceTv.setText("Giá tải: 10.000đ (miễn phí 2 lượt đầu)");
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(MomoPaymentActivity.this, "Lỗi tải dữ liệu sách!", Toast.LENGTH_SHORT).show();
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
                boolean isPaid = snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                if (isPaid) {
                    statusTv.setText("Đã thanh toán ✔️");
                    momoPayBtn.setEnabled(false);
                } else {
                    statusTv.setText("Chưa thanh toán ❌");
                    momoPayBtn.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                statusTv.setText("Không kiểm tra được trạng thái thanh toán");
            }
        });
    }

    private void simulateMomoPayment() {
        // Giả lập người dùng đã thanh toán => cập nhật Firebase
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(currentUid)
                .child("Downloads")
                .child(bookId);

        ref.child("isPaid").setValue(true);
        ref.child("downloadCount").setValue(0); // reset lượt tải nếu muốn

        statusTv.setText("Đã thanh toán ✔️");
        momoPayBtn.setEnabled(false);

        Toast.makeText(this, "Thanh toán thành công!", Toast.LENGTH_SHORT).show();
    }

    private void requestPayment() {
        HashMap<String, Object> event = new HashMap<>();
        event.put("merchantname", "Tên Shop của bạn");
        event.put("merchantcode", "MOMO"); // hoặc mã của bạn nếu có
        event.put("amount", 10000); // Số tiền (int)
        event.put("orderId", UUID.randomUUID().toString());
        event.put("orderLabel", "Thanh toán sách");
        event.put("merchantnamelabel", "Dịch vụ");
        event.put("fee", 0);
        event.put("description", "Thanh toán sách XYZ");
        event.put("requestId", UUID.randomUUID().toString());
        event.put("partnerCode", "MOMO");
        event.put("extra", "bookId=12345"); // hoặc gì bạn muốn

        MomoPay.getInstance().requestMoMoCallBack(this, event);
    }

}

