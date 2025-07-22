package com.example.bookapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bookapp.MyApplication;
import com.example.bookapp.R;
import com.example.bookapp.adapters.AdapterComment;
import com.example.bookapp.adapters.AdapterPdfFavorite;
import com.example.bookapp.databinding.ActivityPdfDetailBinding;
import com.example.bookapp.databinding.DialogCommentAddBinding;
import com.example.bookapp.models.ModelComment;
import com.example.bookapp.models.ModelPdf;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class PdfDetailActivity extends AppCompatActivity {

    //view binding
    private ActivityPdfDetailBinding binding;

    //pdf id, get from intent
    String bookId, bookTitle, bookUrl;
    boolean isInMyFavorite = false;
    private FirebaseAuth firebaseAuth;

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";
    private ProgressDialog progressDialog;

    //arrayList to hold comments
    private ArrayList<ModelComment> commentArrayList;
    //adapter to set to recycleView
    private AdapterComment adapterComment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //get data from intent e.g. bookId
        Intent intent = getIntent();
        bookId = intent.getStringExtra( "bookId");


        //at satart hide download button
        binding.downloadBookBtn.setVisibility(View.GONE);

        //init progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait ...");
        progressDialog.setCanceledOnTouchOutside(false);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            checkIsFavorite();
        }

        loadBookDetails();
        loadComments();
        //increment book view count, whenever this page
        MyApplication.incrementBookViewCount(bookId);


        //handle click, goback
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //handle click, open to view pdf
        binding.readBookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent1 = new Intent(PdfDetailActivity.this, PdfViewActivity.class);
                intent1.putExtra("bookId", bookId);
                startActivity(intent1);
            }
        });

//        //handle click, download pdf
//        binding.downloadBookBtn.setOnClickListener(v -> {
//            Log.d(TAG_DOWNLOAD, "onClick: Bắt đầu tải không cần permission (API >= 29)");
//            MyApplication.downloadBook(
//                    PdfDetailActivity.this,
//                    bookId,
//                    bookTitle,
//                    bookUrl
//            );
//        });
        binding.downloadBookBtn.setOnClickListener(v -> {
            if (firebaseAuth.getCurrentUser() == null) {
                Toast.makeText(this, "Bạn cần đăng nhập để tải sách", Toast.LENGTH_SHORT).show();
                return;
            }

            checkPaymentAndDownload(bookId, bookTitle, bookUrl);
        });

        binding.favoriteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Toast.makeText(PdfDetailActivity.this,"You're not logged in", Toast.LENGTH_SHORT).show();
                } else {
                    if (isInMyFavorite) {
                        // in favorite, remove from favorite
                        MyApplication.removeFromFavorite(PdfDetailActivity.this, bookId);
                    } else {
                        // not in favorite, add to favorite
                        MyApplication.addToFavorite(PdfDetailActivity.this, bookId);
                    }
                }
            }
        });

        binding.addCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(firebaseAuth.getCurrentUser() == null){
                    Toast.makeText(PdfDetailActivity.this, "You're not logged in...", Toast.LENGTH_SHORT).show();
                }
                else{
                    addCommentDialog();
                }
            }
        });
    }

    private void loadComments() {
        //init arraylist before adding data into it
        commentArrayList = new ArrayList<>();

        //db path to load comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Books");
        ref.child(bookId).child("Comments")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //clear arraylist before start adding data into it
                        commentArrayList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            //get data as model, spellings of variables in model must be as same as in firebase
                            ModelComment model = ds.getValue(ModelComment.class);
                            //add to arraylist
                            commentArrayList.add(model);
                        }
                        //setup adapter
                        adapterComment = new AdapterComment(PdfDetailActivity.this, commentArrayList);
                        //set adapter to recyclerview
                        binding.commentsRv.setAdapter(adapterComment);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private String comment ="";
    private void addCommentDialog() {
        //inflate bind view for dialog
        DialogCommentAddBinding commentAddBinding = DialogCommentAddBinding.inflate(LayoutInflater.from(this));

        //setup alert dialog builder
        AlertDialog.Builder builder = new AlertDialog.Builder( this, R.style.CustomDialog);
        builder.setView(commentAddBinding.getRoot());

        //create and show alert dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        //handle click, dismiss dialog
        commentAddBinding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        //handle click, add comment
        commentAddBinding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get data
                comment = commentAddBinding.commentEt.getText().toString().trim();
                // validate data
                if (TextUtils.isEmpty(comment)) {
                    Toast.makeText(PdfDetailActivity.this, "Enter your comment...", Toast.LENGTH_SHORT).show();
                } else {
                    alertDialog.dismiss();
                    addComment();
                }
            }
        });
    }

    private void addComment() {
        //show progress
        progressDialog.setMessage("Adding comment...");
        progressDialog.show();

//timestamp for comment id, comment time
        String timestamp = ""+System.currentTimeMillis();

//setup data to add in db for comment
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", ""+timestamp);
        hashMap.put("bookId", ""+bookId);
        hashMap.put("timestamp", ""+timestamp);
        hashMap.put("comment", ""+comment);
        hashMap.put("uid", ""+firebaseAuth.getUid());

//DB path to add data into it
//Books > bookId > Comments > commentId > commentData
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Books");
        ref.child(bookId).child("Comments").child(timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        progressDialog.dismiss();
                        Toast.makeText(PdfDetailActivity.this, "Comment Added...", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(PdfDetailActivity.this, "Failed to add due to "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }


    // 1. Khai báo launcher để yêu cầu quyền
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG_DOWNLOAD, "Permission Granted");

                    MyApplication.downloadBook(
                            this,
                            "" + bookId,
                            "" + bookTitle,
                            "" + bookUrl
                    );
                }
                else{
                    Log.d(TAG_DOWNLOAD, "Permission was denied...:");
                    Toast.makeText(this, "Permission was denied....", Toast.LENGTH_SHORT).show();
                }
            });


    private void loadBookDetails() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get data
                        bookTitle = ""+snapshot.child("title").getValue();
                        String description = ""+snapshot.child("description").getValue();
                        String categoryId = ""+snapshot.child("categoryId").getValue();
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        String downloadsCount = ""+snapshot.child("downloadsCount").getValue();
                        bookUrl = ""+snapshot.child("url").getValue();
                        String timestamp = ""+snapshot.child("timestamp").getValue();

                        //required data is loaded , show download button
                        binding.downloadBookBtn.setVisibility(View.VISIBLE);



                        //format date
                        String date = MyApplication.formatTimestamp(Long.parseLong(timestamp));

                        MyApplication.loadCategory(
                                 ""+categoryId,
                                binding.categoryTv
            );
                        MyApplication.loadPdfFromUrlSinglePage(
                                ""+bookUrl,
                                 ""+bookTitle,
                                binding.pdfView,
                                binding.progressBar,
                                binding.sizeTv
            );

                        MyApplication.loadPdfSize(
                                ""+bookUrl,
                                ""+bookTitle,
                                binding.sizeTv
            );



                        //set data
                        binding.titleTv.setText(bookTitle);
                        binding.descriptionTv.setText(description);
                        binding.viewsTv.setText(viewsCount.replace("null", "N/A"));
                        binding.downloadsTv.setText(downloadsCount.replace("null", "N/A"));
                        binding.dateTv.setText(date);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }

                });
    }

    private void checkIsFavorite() {
        // logged in check if its in favorite list or not
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Favorites").child(bookId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        isInMyFavorite = snapshot.exists(); // true: if exists, false if not exists
                        if (isInMyFavorite) {
                            // exists in favorite
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_white,0,0);
                            binding.favoriteBtn.setText("Remove Favorite");
                        } else {
                            // not exists in favorite
                            binding.favoriteBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_favorite_border_white,0,0);
                            binding.favoriteBtn.setText("Add Favorite");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    private void checkPaymentAndDownload(String bookId, String bookTitle, String bookUrl) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(firebaseAuth.getUid())
                .child("PurchasedBooks")
                .child(bookId);

        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Long count = snapshot.child("downloadCount").getValue(Long.class);
                if (count == null) count = 0L;

                if (count >= 3) {
                    Toast.makeText(this, "Bạn đã tải sách này 3 lần rồi!", Toast.LENGTH_SHORT).show();
                } else {
                    MyApplication.downloadBook(this, bookId, bookTitle, bookUrl);
                    ref.child("downloadCount").setValue(count + 1);
                }
            } else {
                // Chưa thanh toán → chuyển qua trang thanh toán
                Intent intent = new Intent(this, MomoPaymentActivity.class);
                intent.putExtra("bookId", bookId);
                intent.putExtra("bookTitle", bookTitle);
                intent.putExtra("bookUrl", bookUrl);
                startActivity(intent);
            }
        });
    }


}