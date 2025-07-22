package com.example.bookapp;

import static com.example.bookapp.Constants.MAX_BYTES_PDF;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bookapp.adapters.AdapterPdfAdmin;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import android.content.Context;


//application class runs before your launcher activity
public class MyApplication extends Application {

    private static final String TAG_DOWNLOAD = "DOWNLOAD_TAG";
    @Override
    public void onCreate() {
        super.onCreate();
        // Thiết lập AppCheck với Debug Provider
        com.google.firebase.appcheck.FirebaseAppCheck firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
        );
    }

    //created a static method to convert timestamp to proper date format, so we can use it everywhere in project, no need to rewrite again
    public static final String formatTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp);

        //format timestamp to dd/MM/yyyy
        String date = DateFormat.format("dd/MM/yyyy", cal).toString();

        return date;
    }
    public static void deleteBook(Context context, String bookId, String bookUrl, String bookTitle) {
        String TAG = "DELETE_BOOK_TAG";

        Log.d(TAG, "deleteBook: Deleting...");
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Please wait");
        progressDialog.setMessage("Deleting " + bookTitle + "...");
        progressDialog.show();

        Log.d(TAG, "deleteBook: Deleting from storage...");
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageReference.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Deleted from storage");

                        Log.d(TAG, "onSuccess: Now deleting info from db");
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                        reference.child(bookId)
                                .removeValue()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "onSuccess: Deleted from db too");
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "Book Deleted Successfully...", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "onFailure: Failed to delete from db due to " + e.getMessage());
                                        progressDialog.dismiss();
                                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: Failed to delete from storage due to " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(context, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    public static void loadPdfSize(String pdfUrl, String pdfTitle, TextView sizeTv) {
        String TAG="PDF_SIZE_TAG";
        //using url we can get file and its metadata from firebase storage


        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getMetadata()
                .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                    @Override
                    public void onSuccess(StorageMetadata storageMetadata) {
                        //get size in bytes
                        double bytes = storageMetadata.getSizeBytes();
                        Log.d(TAG, "onSuccess: "+pdfTitle +" "+bytes);
                        //convert bytes to KB, MB
                        double kb = bytes / 1024;
                        double mb = kb / 1024;

                        if (mb > 1) {
                            sizeTv.setText(String.format("%.2f", mb) + " MB");
                        }
                        else if (kb > 1) {
                            sizeTv.setText(String.format("%.2f", kb) + " KB");
                        }
                        else {
                            sizeTv.setText(String.format("%.2f", bytes) + " bytes");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed getting metadata
                        Log.d(TAG, "onFailure: "+e.getMessage());
                    }
                });
    }

    public static void loadPdfFromUrlSinglePage(String pdfUrl, String pdfTitle, PDFView pdfView, ProgressBar progressBar, TextView pagesTv) {
        String TAG="PDF_LOAD_SINGLE_TAG";

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
        ref.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG, "onSuccess: " + pdfTitle + " successfully got the file");

                        //set to pdfview
                        pdfView.fromBytes(bytes)
                                .pages(0) //show only first page
                                .spacing(0)
                                .swipeHorizontal(false)
                                .enableSwipe(false)
                                .onError(new OnErrorListener() {
                                    @Override
                                    public void onError(Throwable t) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG,"onError: " + t.getMessage());
                                    }
                                })
                                .onPageError(new OnPageErrorListener() {
                                    @Override
                                    public void onPageError(int page, Throwable t) {
                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "onPageError: " + t.getMessage());
                                    }
                                })
                                .onLoad(new OnLoadCompleteListener() {
                                    @Override
                                    public void loadComplete(int nbPages) {

                                        progressBar.setVisibility(View.INVISIBLE);
                                        Log.d(TAG, "loadComplete: pdf loaded");

                                        if (pagesTv != null) {
                                            pagesTv.setText("" + nbPages); // concatenate with double quotes because can't set int in textview
                                        }
                                    }
                                })
                                .load();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: failed getting file from url due to " + e.getMessage());
                    }
                });
    }

    public static void loadCategory(String categoryId, TextView categoryTv ) {
        //get category using categoryId

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Categories");
        ref.child(categoryId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get category
                        String category = "" + snapshot.child("category").getValue();

                        //set to category text view
                        categoryTv.setText(category);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    public static void incrementBookViewCount(String bookId) {
        //1) Get book views count
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Books");
        ref.child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //get views count
                        String viewsCount = ""+snapshot.child("viewsCount").getValue();
                        //in case of null replace with 0
                        if (viewsCount.equals("") || viewsCount.equals("null")) {
                            viewsCount = "0";
                        }

                        //2)Increment views count
                        long newViewsCount = Long.parseLong(viewsCount) + 1;

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("viewsCount", newViewsCount);

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference( "Books");
                        reference.child(bookId)
                                .updateChildren(hashMap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

//    public static void downloadBook(Context context, String bookId, String bookTitle, String bookUrl) {
//        Log.d(TAG_DOWNLOAD, "downloadBook: bắt đầu...");
//
//        // Tạo tên file PDF từ tiêu đề sách
//        String fileName = bookTitle + ".pdf";
//        Log.d(TAG_DOWNLOAD, "Tên file tải xuống: " + fileName);
//
//        // Hiển thị ProgressDialog
//        ProgressDialog progressDialog = new ProgressDialog(context);
//        progressDialog.setTitle("Vui lòng chờ");
//        progressDialog.setMessage("Đang tải xuống: " + fileName);
//        progressDialog.setCanceledOnTouchOutside(false);
//        progressDialog.show();
//
//        // Tải từ Firebase Storage
//        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
//        storageRef.getBytes(MAX_BYTES_PDF)
//                .addOnSuccessListener(bytes -> {
//                    Log.d(TAG_DOWNLOAD, "Tải thành công: Book Downloaded");
//                    Log.d(TAG_DOWNLOAD, "Đang lưu sách...");
//
//                    saveDownloadedBook(context, progressDialog, bytes, fileName, bookId);
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG_DOWNLOAD, "Lỗi tải file: " + e.getMessage());
//                    progressDialog.dismiss();
//                    Toast.makeText(context, "Tải thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                });
//    }


    public static void downloadBook(Context context, String bookId, String bookTitle, String bookUrl) {
        Log.d(TAG_DOWNLOAD, "downloadBook: bắt đầu...");

        String fileName = bookTitle + ".pdf";
        Log.d(TAG_DOWNLOAD, "Tên file tải xuống: " + fileName);

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Vui lòng chờ");
        progressDialog.setMessage("Đang tải xuống: " + fileName);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(bookUrl);
        storageRef.getBytes(MAX_BYTES_PDF)
                .addOnSuccessListener(bytes -> {
                    Log.d(TAG_DOWNLOAD, "Tải thành công, đang lưu sách...");
                    savePdfToDownloads(context, bytes, fileName);
                    progressDialog.dismiss();
                    incrementBookDownloadCount(bookId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG_DOWNLOAD, "Lỗi tải file: " + e.getMessage());
                    progressDialog.dismiss();
                    Toast.makeText(context, "Tải thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public static void savePdfToDownloads(Context context, byte[] bytes, String fileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29+ → dùng MediaStore
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                ContentResolver resolver = context.getContentResolver();
                Uri fileUri = resolver.insert(collection, values);

                if (fileUri != null) {
                    try (OutputStream out = resolver.openOutputStream(fileUri)) {
                        out.write(bytes);
                        out.flush();
                    }

                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(fileUri, values, null, null);

                    Toast.makeText(context, "Đã lưu vào Downloads (API 29+)", Toast.LENGTH_SHORT).show();
                    Log.d(TAG_DOWNLOAD, "Đã lưu bằng MediaStore: " + fileName);
                }

            } catch (Exception e) {
                Log.e(TAG_DOWNLOAD, "Lỗi lưu bằng MediaStore: " + e.getMessage());
                Toast.makeText(context, "Lỗi MediaStore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // API < 29 → fallback sang cách cũ (cần WRITE_EXTERNAL_STORAGE)
            savePdfToDownloadsLegacy(context, bytes, fileName);
        }
    }

    private static void savePdfToDownloadsLegacy(Context context, byte[] bytes, String fileName) {
        try {
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs();
            }

            File file = new File(downloadsFolder, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();

            Toast.makeText(context, "Đã lưu vào Downloads (legacy)", Toast.LENGTH_SHORT).show();
            Log.d(TAG_DOWNLOAD, "Lưu bằng legacy: " + file.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(context, "Lỗi lưu legacy: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG_DOWNLOAD, "Lỗi legacy: " + e.getMessage());
        }
    }

    private static void saveDownloadedBook(Context context, ProgressDialog progressDialog, byte[] bytes, String nameWithExtension, String bookId) {
        Log.d(TAG_DOWNLOAD, "saveDownloadedBook: bắt đầu lưu sách...");

        try {
            // Tạo thư mục Download mặc định
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs();
            }

            // Đường dẫn đến file
            String filePath = new File(downloadsFolder, nameWithExtension).getAbsolutePath();

            // Ghi dữ liệu ra file
            FileOutputStream out = new FileOutputStream(filePath);
            out.write(bytes);
            out.close();

            // Hiển thị kết quả thành công
            Toast.makeText(context, "Đã lưu vào thư mục Downloads", Toast.LENGTH_SHORT).show();
            Log.d(TAG_DOWNLOAD, "Đã lưu file: " + filePath);

            // Đóng progress dialog
            progressDialog.dismiss();

            // Cập nhật số lượt tải
            incrementBookDownloadCount(bookId);

        } catch (Exception e) {
            Log.e(TAG_DOWNLOAD, "Lỗi lưu sách: " + e.getMessage());
            Toast.makeText(context, "Lưu file thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    private static void incrementBookDownloadCount(String bookId) {
        Log.d(TAG_DOWNLOAD, "Incrementing Book Download Count...");

        // Step 1: Lấy dữ liệu hiện tại
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Books");

        ref.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String downloadsCountStr = "" + snapshot.child("downloadsCount").getValue();
                Log.d(TAG_DOWNLOAD, "onDataChange: Downloads Count = " + downloadsCountStr);

                // Nếu rỗng hoặc null thì gán = 0
                if (downloadsCountStr.equals("") || downloadsCountStr.equals("null")) {
                    downloadsCountStr = "0";
                }

                // Tăng 1 lượt tải
                long newDownloadsCount = Long.parseLong(downloadsCountStr) + 1;
                Log.d(TAG_DOWNLOAD, "onDataChange: New Downloads Count = " + newDownloadsCount);

                // Tạo HashMap để update
                HashMap<String, Object> updateMap = new HashMap<>();
                updateMap.put("downloadsCount", newDownloadsCount);

                // Cập nhật lại vào Firebase
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Books");
                reference.child(bookId).updateChildren(updateMap)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d(TAG_DOWNLOAD, "onSuccess: Downloads Count updated!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG_DOWNLOAD, "onFailure: Failed to update Downloads Count - " + e.getMessage());
                            }
                        });
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG_DOWNLOAD, "Lỗi đọc dữ liệu: " + error.getMessage());
            }
        });
    }

    //video 12 phút 26:24 xóa
//    public static void loadPdfPageCount(Context context, String pdfUrl, TextView pagesTv) {
//        // load pdf file from firebase storage using url
//        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(pdfUrl);
//
//        storageReference
//                .getBytes(Constants.MAX_BYTES_PDF)
//                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
//                    @Override
//                    public void onSuccess(byte[] bytes) {
//                        // file received
//
//                        // load pdf pages using PdfView library
//                        PDFView pdfView = new PDFView(context, null);
//                        pdfView.fromBytes(bytes)
//                                .onLoad(new OnLoadCompleteListener() {
//                                    @Override
//                                    public void loadComplete(int nbPages) {
//                                        // pdf loaded from bytes we got from firebase storage, we can now show number of pages
//                                        pagesTv.setText("" + nbPages);
//                                    }
//                                });
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        // file failed to receive
//                    }
//                });
//    }

    public static void addToFavorite(Context context, String bookId) {
        // we can add only if user is logged in
        // 1)Check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            // not logged in, cant add to fav
            Toast.makeText(context, "You're not logged in", Toast.LENGTH_SHORT).show();
        } else {
            long timestamp = System.currentTimeMillis();

            // setup data to add in firebase db of current user for favorite book
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("bookId", ""+bookId);
            hashMap.put("timestamp", ""+timestamp);

            // save to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference( "Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Added to your favoriteslisist...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to add to favorite due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public static void removeFromFavorite(Context context, String bookId) {
        // we can add remove if user is logged in
        // 1)Check if user is logged in
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            // not logged in, cant remove from fav
            Toast.makeText(context, "You're not logged in", Toast.LENGTH_SHORT).show();
        } else {
            // remove from db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Favorites").child(bookId).removeValue()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(context, "Removed from your favorites list...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(context, "Failed to remove from favorite due to " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}
