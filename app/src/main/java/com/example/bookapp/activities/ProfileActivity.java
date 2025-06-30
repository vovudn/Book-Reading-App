package com.example.bookapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapters.AdapterPdfFavorite;
import com.example.bookapp.databinding.ActivityProfileBinding;
import com.example.bookapp.models.ModelPdf;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    private FirebaseAuth firebaseAuth;

    //arraylist to holde the books
    private ArrayList<ModelPdf> pdfArrayList;

    //adapter to set in recycler
    private AdapterPdfFavorite adapterPdfFavorite;
    private static final String TAG = "PROFILE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // setup firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadUserInfo();
        loadFavoriteBook();

        binding.profileEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ProfileActivity.this, ProfileEditActivity.class));
            }
        });

        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }

    private void loadUserInfo() {
        Log.d(TAG, "loadUserInfo: Loading user info of user " + firebaseAuth.getUid());

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference( "Users");
        reference.child(firebaseAuth.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // get all info from user from snapshot
                        String email = "" + snapshot.child("email").getValue();
                        String name = "" + snapshot.child("name").getValue();
                        String profileImage = "" + snapshot.child("profileImage").getValue();
                        String timestamp = "" + snapshot.child("timestamp").getValue();
                        String uid = "" + snapshot.child("uid").getValue();
                        String userType = "" + snapshot.child("userType").getValue();

                        // format date to dd/MM/yyyy
                        String formattedDate = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        // set data to ui
                        binding.emailTv.setText(email);
                        binding.nameTv.setText(name);
                        binding.memberDataTv.setText(formattedDate);
                        binding.accountTypeTv.setText(userType);

                        // set image, using glide
                        Glide.with(ProfileActivity.this)
                                .load(profileImage)
                            .placeholder(R.drawable.ic_person_gray)
                                .into(binding.profileIv);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }
    private void loadFavoriteBook(){
        // init list
        pdfArrayList = new ArrayList<>();

        //load favorite books from database
        //Users > userId > Favorites
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Users");
        ref.child(firebaseAuth.getUid()).child("Favorites")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear list before starting adding data
                        pdfArrayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //we will only get the bookId here, and we got other details in adapter using that bookId
                            String bookId = "" + ds.child("bookId").getValue();

                            //set id to model
                            ModelPdf modelPdf = new ModelPdf();
                            modelPdf.setId(bookId);

                            //add model to list
                            pdfArrayList.add(modelPdf);
                        }
                        //set number of favorite books
                        binding.favoriteBookCountTv.setText("" + pdfArrayList.size());//can't set int/long to textview so concatenate with string
                        //setup adapter
                        adapterPdfFavorite = new AdapterPdfFavorite( ProfileActivity.this, pdfArrayList);
                        //set adapter to recyclerview
                        binding.booksRv.setAdapter(adapterPdfFavorite);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
}