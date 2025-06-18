package com.example.bookapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.adapters.AdapterCategory;
import com.example.bookapp.databinding.ActivityDashboardAdminBinding;
import com.example.bookapp.models.ModelCategory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class DashboardAdminActivity extends AppCompatActivity {

    // View binding
    private ActivityDashboardAdminBinding binding;

    // Firebase auth
    private FirebaseAuth firebaseAuth;

    // Category list and adapter
    private ArrayList<ModelCategory> categoryArrayList;
    private AdapterCategory adapterCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Check user and load categories
        checkUser();
        loadCategories();


        // edit text change listener, search
        binding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không cần xử lý gì trước khi text thay đổi
                try {
                    adapterCategory.getFilter().filter(s);
                } catch (Exception e) {

                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // called as and when user type each letter
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Xử lý sau khi người dùng thay đổi nội dung ô tìm kiếm
            }
        });

        // Logout button click listener
        binding.logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                checkUser();
            }
        });

        // Add category button click listener
        binding.addCategoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardAdminActivity.this, CategoryAddActivity.class));
            }
        });

        //handle click, start pdf add screen
        binding.addPdfFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardAdminActivity.this, PdfAddActivity.class));
            }
        });
    }





    private void loadCategories() {
        categoryArrayList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://hellodemo-8dae1-default-rtdb.firebaseio.com/")
                .getReference("Categories");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                categoryArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelCategory model = ds.getValue(ModelCategory.class);
                    categoryArrayList.add(model);
                }

                adapterCategory = new AdapterCategory(DashboardAdminActivity.this, categoryArrayList);
                binding.categoriesRv.setAdapter(adapterCategory);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors here if needed
            }
        });
    }

    private void checkUser() {
        // Get current user
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            // Not logged in, go to main screen
            startActivity(new Intent(this, MainActivity.class));
            finish();
        } else {
            // Show user email
            String email = firebaseUser.getEmail();
            binding.subTitleTv.setText(email);
        }
    }
}

