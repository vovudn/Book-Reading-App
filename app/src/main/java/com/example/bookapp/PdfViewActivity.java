package com.example.bookapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.bookapp.databinding.ActivityPdfViewBinding;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class PdfViewActivity extends AppCompatActivity {

    // view binding
    private ActivityPdfViewBinding binding;

    private String bookId;

    private static final String TAG = "PDF_VIEW_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // get bookId from intent that we passed in intent
        Intent intent = getIntent();
        bookId = intent.getStringExtra("bookId");
        Log.d(TAG, "onCreate: BookId: " + bookId);

        loadBookDetails();

        // handle click, go back
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void loadBookDetails() {
        Log.d(TAG, "loadBookDetails: Get PDF URL from db....");
        // Database Reference to get book details e.g. get book url using book id
        // Step (1) Get Book Url using Book Id
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // get book url
                        String pdfUrl = ""+snapshot.child("url").getValue();

                        //Step 2 : Load PDF using that url from firebase storage
                        loadBookFromUrl(pdfUrl);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // handle error here (optional)
                    }
                });
    }

    private void loadBookFromUrl(String pdfUrl) {
        Log.d(TAG, "loadBookFromUrl: GetPDF from storage");
        StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        reference.getBytes(Constants.MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        // load pdf using bytes
                        binding.pdfView.fromBytes(bytes)
                                .swipeHorizontal(false) // set false to scroll vertical, true to swipe horizontal
                                .onPageChange(new OnPageChangeListener() {
                                    @Override
                                    public void onPageChanged(int page, int pageCount) {
                                        // Set current page and total pages in toolbar subtitle
                                        int currentPage = page + 1; // page starts from 0
                                        binding.toolbarSubtitleTv.setText(currentPage + "/" + pageCount);
                                        Log.d(TAG, "onPageChanged:" +currentPage + "/" + pageCount);
                                    }
                                })
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        Log.d(TAG, "onError: "+t.getMessage());
                                        Toast.makeText(PdfViewActivity.this, "" + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        Log.d(TAG, "onError: "+t.getMessage());
                                        Toast.makeText(PdfViewActivity.this, "Error on page" + page+""+t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .load();


                        binding.progressBar.setVisibility(View.GONE);
                    }
                }).addOnFailureListener(new OnFailureListener() {

                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " +e.getMessage());
                        //failed to load book
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });
    }


}
