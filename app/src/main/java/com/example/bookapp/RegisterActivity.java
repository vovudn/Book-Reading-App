package com.example.bookapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bookapp.databinding.ActivityRegisterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    // View Binding
    private ActivityRegisterBinding binding;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();


        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        // Handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed(); // Quay lại màn hình trước
            }
        });

        // Handle click, begin register
        binding.registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateData();
            }
        });
    }

    private String name = "", email = "", password = "";

    // Validate input data before creating account
    private void validateData() {
        // Get data from EditTexts
        name = binding.nameEt.getText().toString().trim();
        email = binding.emailEt.getText().toString().trim();
        password = binding.passwordEt.getText().toString().trim();
        String cPassword = binding.cPasswordEt.getText().toString().trim();

        // validate data
        if (TextUtils.isEmpty(name)) {
            // Name edit text is empty, must enter name
            Toast.makeText(this, "Enter your name...", Toast.LENGTH_SHORT).show();

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Email is invalid or empty
            Toast.makeText(this, "Invalid email pattern...!", Toast.LENGTH_SHORT).show();

        } else if (TextUtils.isEmpty(password)) {
            // Password edit text is empty, must enter password
            Toast.makeText(this, "Enter password...!", Toast.LENGTH_SHORT).show();

        } else if (TextUtils.isEmpty(cPassword)) {
            // Confirm password edit text is empty, must enter confirm password
            Toast.makeText(this, "Confirm password...!", Toast.LENGTH_SHORT).show();

        } else if (!password.equals(cPassword)) {
            // Password and confirm password doesn't match
            Toast.makeText(this, "Password doesn't match...!", Toast.LENGTH_SHORT).show();

        } else {
            // All data is validated, begin creating account
            createUserAccount();
        }

    }

//    private void createUserAccount() {
//        progressDialog.setMessage("Creating account...");
//        progressDialog.show();
//
//        // Create user in Firebase Auth
//        firebaseAuth.createUserWithEmailAndPassword(email, password)
//                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
//                    @Override
//                    public void onSuccess(AuthResult authResult) {
//                        // Lấy UID từ đối tượng người dùng vừa tạo
//                        String uid = authResult.getUser().getUid();
//                        updateUserInfo(uid);  // Truyền UID vào
//                    }
//
//
//
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(Exception e) {
//                        // Account creating failed
//                        progressDialog.dismiss();
//                        Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//    }



    private void createUserAccount() {
        progressDialog.setMessage("Creating account...");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Lấy UID từ user vừa tạo
                    String uid = authResult.getUser().getUid();
                    updateUserInfo(uid); // Truyền UID đúng
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

//    private void updateUserInfo() {
//        // Timestamp
//        long timestamp = System.currentTimeMillis();
//
//        // Get current user UID (since user is registered, we can get now)
//        String uid = firebaseAuth.getUid();
//
//        // Setup data to add in database
//        HashMap<String, Object> hashMap = new HashMap<>();
//        hashMap.put("uid", uid);
//        hashMap.put("email", email);
//        hashMap.put("name", name);
//        hashMap.put("profileImage", ""); // Empty for now, can update later
//        hashMap.put("userType", "user"); // Possible values: user, admin
//        hashMap.put("timestamp", timestamp);
//
//        // Set data to Firebase Realtime Database
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
//        ref.child(uid)
//                .setValue(hashMap)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void unused) {
//                        // Data added to database successfully
//                        progressDialog.dismiss();
//                        Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
//                        startActivity(new Intent(RegisterActivity.this, DashboardUserActivity.class));
//                        finish();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(Exception e) {
//                        // Failed to add data to database
//                        progressDialog.dismiss();
//                        Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }

//    private void updateUserInfo(String uid) {
//        long timestamp = System.currentTimeMillis();
//
//        HashMap<String, Object> hashMap = new HashMap<>();
//        hashMap.put("uid", uid);
//        hashMap.put("email", email);
//        hashMap.put("name", name);
//        hashMap.put("profileImage", "");
//        hashMap.put("userType", "user");
//        hashMap.put("timestamp", timestamp);
//
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
//        ref.child(uid)
//                .setValue(hashMap)
//                .addOnSuccessListener(unused -> {
//                    progressDialog.dismiss();
//                    Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
//                    startActivity(new Intent(RegisterActivity.this, DashboardUserActivity.class));
//                    finish();
//                })
//                .addOnFailureListener(e -> {
//                    progressDialog.dismiss();
//                    Toast.makeText(RegisterActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }

    private void updateUserInfo(String uid) {
        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid", uid);
        hashMap.put("email", email);
        hashMap.put("name", name);
        hashMap.put("profileImage", "");
        hashMap.put("userType", "user");
        hashMap.put("timestamp", timestamp);

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://hellodemo-8dae1-default-rtdb.firebaseio.com/")
                .getReference("Users");

        ref.child(uid).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, DashboardUserActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(RegisterActivity.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
