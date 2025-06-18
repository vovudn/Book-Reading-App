    package com.example.bookapp;

    import android.Manifest;
    import android.app.ProgressDialog;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.net.Uri;
    import android.os.Build;
    import android.os.Bundle;
    import android.text.TextUtils;
    import android.util.Log;
    import android.view.View;
    import android.widget.Toast;

    import androidx.activity.EdgeToEdge;
    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.app.ActivityCompat;
    import androidx.core.app.NotificationCompat;
    import androidx.core.app.NotificationManagerCompat;
    import androidx.core.content.ContextCompat;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowInsetsCompat;

    import com.example.bookapp.databinding.ActivityPdfAddBinding;
    import com.google.android.gms.tasks.OnFailureListener;
    import com.google.android.gms.tasks.OnSuccessListener;
    import com.google.android.gms.tasks.Task;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.database.DataSnapshot;
    import com.google.firebase.database.DatabaseError;
    import com.google.firebase.database.DatabaseReference;
    import com.google.firebase.database.FirebaseDatabase;
    import com.google.firebase.database.ValueEventListener;
    import com.google.firebase.database.annotations.NotNull;
    import com.google.firebase.storage.FirebaseStorage;
    import com.google.firebase.storage.StorageReference;
    import com.google.firebase.storage.UploadTask;

    import java.util.ArrayList;
    import java.util.HashMap;


    public class PdfAddActivity extends AppCompatActivity {

        private ActivityPdfAddBinding binding;

        private FirebaseAuth firebaseAuth;

        private ProgressDialog progressDialog;

        private ArrayList<String> categoryTitleArrayList, categoryIdArrayList;


        private Uri pdfUri = null;

        private static final int PDF_PICK_CODE = 1000;

        private static final String TAG = "ADD_PDF_TAG";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            binding = ActivityPdfAddBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            firebaseAuth = FirebaseAuth.getInstance();
            loadPdfCategories();

            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Please wait");
            progressDialog.setCanceledOnTouchOutside(false);

            binding.backBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
    //                onBackPressed();
                    finish();
                }
            });

            binding.attachBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public  void onClick(View v){
                    pdfPickIntent();
                }

            });

            binding.categoryTv.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    categoryPickDialog();
                }
            });

            binding.submitBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    validateData();
                }

            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            1001
                    );
                }
            }

        }

        private String title= "", description = "";
        private void validateData() {

            Log.d(TAG, "validateData: validating data...");

            title = binding.titleEt.getText().toString().trim();
            description = binding.descriptionEt.getText().toString().trim();


            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Enter Title...", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(description)) {
                Toast.makeText(this, "Enter Description...", Toast.LENGTH_SHORT).show();
            }
            else if (TextUtils.isEmpty(selectedCategoryTitle)) {
                Toast.makeText(this, "Pick Category...", Toast.LENGTH_SHORT).show();
            }
            else if (pdfUri == null) {
                Toast.makeText(this, "Pick Pdf...", Toast.LENGTH_SHORT).show();
            }
            else {
                // all data is valid, can upload now
                uploadPdfToStorage();
            }

        }

        private void uploadPdfToStorage() {
            Log.d(TAG, "uploadPdfToStorage: uploading to storage...");

            progressDialog.setMessage("Uploading Pdf...");
            progressDialog.show();

            long timestamp = System.currentTimeMillis();
            String filePathAndName = "Books/" + timestamp;

            // Storage reference
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(pdfUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Log.d(TAG, "onSuccess: PDF uploaded to storage...");
                            Log.d(TAG, "onSuccess: getting pdf url");

                            // Get download URL
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            String uploadPdfUrl = ""+uriTask.getResult();

                            uploadPdfInfoToDb(uploadPdfUrl, timestamp);
    //                        uriTask.addOnSuccessListener(uri -> {
    //                            String uploadedPdfUrl = uri.toString();
    //                            uploadPdfInfoToDb(uploadedPdfUrl);
    //                            progressDialog.dismiss();
    //                        });

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressDialog.dismiss();
                            Log.d(TAG, "onFailure: PDF upload failed due to " + e.getMessage());
                            Toast.makeText(PdfAddActivity.this, "PDF upload failed due to " + e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
        }

//        private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
//            Log.d(TAG, "uploadPdfInfoToDb: Uploading Pdf info to firebase db...");
//            progressDialog.setMessage("Uploading pdf info...");
//            String uid = firebaseAuth.getUid();
//
//            //setup data to upload
//            HashMap<String, Object> hashMap = new HashMap<>();
//            hashMap.put("uid", "" + uid);
//            hashMap.put("id", "" + timestamp);
//            hashMap.put("title", "" + title);
//            hashMap.put("description", "" + description);
//            hashMap.put("categoryId", "" + selectedCategoryId);
//            hashMap.put("url", "" + uploadedPdfUrl);
//            hashMap.put("timestamp", timestamp);
//            hashMap.put("viewsCount",0);
//            hashMap.put("downloadsCount",0);
//
//            DatabaseReference ref = FirebaseDatabase
//                    .getInstance("https://hellodemo-8dae1-default-rtdb.firebaseio.com/")
//                    .getReference()
//                    .child("Books");
//
//            ref.child("" + timestamp).setValue(hashMap)
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void unused) {
//                            progressDialog.dismiss();
//                            Log.d(TAG,"onSuccess: Successfully uploaded...");
//                            Toast.makeText(PdfAddActivity.this, "Successfully uploaded...", Toast.LENGTH_SHORT).show();
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            progressDialog.dismiss();
//                            Log.d(TAG,"onFailure: Failed to upload to db due to " + e.getMessage());
//                            Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
//
//                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
//                                    ContextCompat.checkSelfPermission(PdfAddActivity.this, Manifest.permission.POST_NOTIFICATIONS)
//                                            == PackageManager.PERMISSION_GRANTED) {
//
//                                Log.d(TAG, "🔔 Notification should appear now");
//
//                                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
//                                        .setSmallIcon(R.drawable.ic_notification)
//                                        .setContentTitle("Book Added!")
//                                        .setContentText("The book \"" + title + "\" was successfully added.")
//                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
//                                        .setDefaults(NotificationCompat.DEFAULT_ALL)
//                                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
//
//                                NotificationManagerCompat manager = NotificationManagerCompat.from(this);
//                                manager.notify(1, builder.build());
//
//
//
//                            } else {
//                                Log.w(TAG, "Notification permission not granted");
//                            }
//
//
//                        }
//                    });
//        }


        private void uploadPdfInfoToDb(String uploadedPdfUrl, long timestamp) {
            Log.d(TAG, "uploadPdfInfoToDb: Uploading Pdf info to firebase db...");
            progressDialog.setMessage("Uploading pdf info...");
            String uid = firebaseAuth.getUid();

            //setup data to upload
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("uid", "" + uid);
            hashMap.put("id", "" + timestamp);
            hashMap.put("title", "" + title);
            hashMap.put("description", "" + description);
            hashMap.put("categoryId", "" + selectedCategoryId);
            hashMap.put("url", "" + uploadedPdfUrl);
            hashMap.put("timestamp", timestamp);
            hashMap.put("viewsCount",0);
            hashMap.put("downloadsCount",0);

            DatabaseReference ref = FirebaseDatabase
                    .getInstance("https://hellodemo-8dae1-default-rtdb.firebaseio.com/")
                    .getReference("Books");

            ref.child("" + timestamp).setValue(hashMap)
                    .addOnSuccessListener(unused -> {
                        progressDialog.dismiss();
                        Log.d(TAG,"onSuccess: Successfully uploaded...");
                        Toast.makeText(PdfAddActivity.this, "Successfully uploaded...", Toast.LENGTH_SHORT).show();

                        // 🔔 Send notification when success
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                ContextCompat.checkSelfPermission(PdfAddActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                                        == PackageManager.PERMISSION_GRANTED) {

                            NotificationCompat.Builder builder = new NotificationCompat.Builder(PdfAddActivity.this, MyApplication.CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_notification)
                                    .setContentTitle("Book Added!")
                                    .setContentText("The book \"" + title + "\" was successfully added.")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                            NotificationManagerCompat manager = NotificationManagerCompat.from(PdfAddActivity.this);
                            manager.notify(1, builder.build());

                        } else {
                            Log.w(TAG, "Notification permission not granted");
                        }

                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Log.d(TAG,"onFailure: Failed to upload to db due to " + e.getMessage());
                        Toast.makeText(PdfAddActivity.this, "Failed to upload to db due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == 1001) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission granted");
                } else {
                    Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void loadPdfCategories() {
            Log.d(TAG, "loadPdfCategories: Loading pdf categories...");
            categoryTitleArrayList = new ArrayList<>();
            categoryIdArrayList = new ArrayList<>();
            // db reference to load categories... db > Categories
            DatabaseReference ref = FirebaseDatabase
                    .getInstance("https://hellodemo-8dae1-default-rtdb.firebaseio.com/")
                    .getReference()
                    .child("Categories");
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NotNull DataSnapshot snapshot) {
                    categoryTitleArrayList.clear(); // clear before adding data
                    categoryIdArrayList.clear();
                    for (DataSnapshot ds : snapshot.getChildren()) {

                        String categoryId = ""+ds.child("id").getValue();
                        String categoryTitle = ""+ds.child("category").getValue();

                        categoryTitleArrayList.add(categoryTitle);
                        categoryIdArrayList.add(categoryId);
                    }
                }

                @Override
                public void onCancelled(@NotNull DatabaseError error) {
                    // Xử lý lỗi nếu cần
                }
            });
        }

        private String selectedCategoryId, selectedCategoryTitle;
        private void categoryPickDialog() {
            Log.d(TAG, "categoryPickDialog: showing category pick dialog");

            // get string array of categories from arraylist
            String[] categoriesArray = new String[categoryTitleArrayList.size()];
            for (int i = 0; i < categoryTitleArrayList.size(); i++) {
                categoriesArray[i] = categoryTitleArrayList.get(i);
            }

            // alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick Category")
                    .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // handle item click
                            // get clicked item from list
                            selectedCategoryTitle = categoryTitleArrayList.get(which);
                            selectedCategoryId = categoryIdArrayList.get(which);
                            binding.categoryTv.setText(selectedCategoryTitle);

                            Log.d(TAG, "onClick: Selected Category: "+selectedCategoryId+" "+selectedCategoryTitle);
                        }
                    })
                    .show();
        }

    //    private void pdfPickIntent() {
    //        Log.d(TAG, "pdfPickIntent: starting pdf pick intent");
    //
    //        Intent intent = new Intent();
    //        intent.setType("application/pdf");
    //        intent.setAction(Intent.ACTION_GET_CONTENT);
    //        startActivityForResult(Intent.createChooser(intent, "Select Pdf"), PDF_PICK_CODE);
    //    }

    //    private void pdfPickIntent() {
    //        Log.d(TAG, "pdfPickIntent: starting pdf pick intent");
    //
    //        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    //        intent.setType("application/pdf");
    //        intent.addCategory(Intent.CATEGORY_OPENABLE);
    //        startActivityForResult(intent, PDF_PICK_CODE);
    //    }

        private void pdfPickIntent() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION); // ✅ cực kỳ quan trọng
            startActivityForResult(intent, PDF_PICK_CODE);
        }





    //    @Override
    //    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    //        super.onActivityResult(requestCode, resultCode, data);
    //
    //        if (resultCode == RESULT_OK) {
    //            if (requestCode == PDF_PICK_CODE) {
    //                Log.d(TAG, "onActivityResult: PDF picked");
    //
    //                pdfUri = data.getData();
    //
    //                Log.d(TAG, "onActivityResult: URI: " + pdfUri);
    //            }
    //        }
    //        else {
    //            Log.d(TAG, "onActivityResult: cancelled picking pdf");
    //            Toast.makeText(this,"cancelled picking pdf", Toast.LENGTH_SHORT).show();
    //        }
    //    }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (resultCode == RESULT_OK && requestCode == PDF_PICK_CODE && data != null) {
                pdfUri = data.getData();
                Log.d(TAG, "onActivityResult: URI: " + pdfUri);

                try {
                    getContentResolver().takePersistableUriPermission(
                            pdfUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception e) {
                    Log.e(TAG, "Permission persist error: " + e.getMessage());
                }

                Toast.makeText(this, "PDF selected", Toast.LENGTH_SHORT).show();
            }
        }





    }