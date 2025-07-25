package com.example.bookapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bookapp.MyApplication;
import com.example.bookapp.activities.PdfDetailActivity;
import com.example.bookapp.databinding.RowPdfUserBinding;
import com.example.bookapp.filters.FilterPdfUser;
import com.example.bookapp.models.ModelPdf;
import com.github.barteksc.pdfviewer.PDFView;

import java.util.ArrayList;

//public class AdapterPdfUser extends RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser> implements Filterable {
//
//    private Context context;
//    public ArrayList<ModelPdf> pdfArrayList, filterList;
//    private RowPdfUserBinding binding;
//    private FilterPdfUser filter;
//
//    public boolean isFiltering = false;
//
//    private static final String TAG = "ADAPTER_PDF_USER_TAG";
//
//    public AdapterPdfUser(Context context, ArrayList<ModelPdf> pdfArrayList) {
//        this.context = context;
//        this.pdfArrayList = pdfArrayList;
//        this.filterList = new ArrayList<>(pdfArrayList);;
//    }
//
//    @NonNull
//    @Override
//    public HolderPdfUser onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        // Bind the view using ViewBinding
//        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false);
//        return new HolderPdfUser(binding.getRoot());
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull AdapterPdfUser.HolderPdfUser holder, int position) {
//        // Get data model
//        ModelPdf model = pdfArrayList.get(position);
//        String bookId = model.getId();
//        String title = model.getTitle();
//        String description = model.getDescription();
//        String pdfUrl = model.getUrl();
//        String categoryId = model.getCategoryId();
//        long timestamp = model.getTimestamp();
//
//        // Convert timestamp to readable date
//        String date = MyApplication.formatTimestamp(timestamp);
//
//        // Set data
//        holder.titleTv.setText(title);
//        holder.descriptionTv.setText(description);
//        holder.dateTv.setText(date);
//
////        // Load one page preview of PDF
////        MyApplication.loadPdfFromUrlSinglePage(
////                "" + pdfUrl,
////                "" + title,
////                holder.pdfView,
////                holder.progressBar,
////                null
////        );
//
//        if (filter == null || constraint == null || constraint.length() == 0) {
//            MyApplication.loadPdfFromUrlSinglePage(
//                    "" + pdfUrl,
//                    "" + title,
//                    holder.pdfView,
//                    holder.progressBar,
//                    null
//            );
//        } else {
//            // Khi đang tìm kiếm, ẩn pdfView để tránh crash
//            holder.pdfView.setVisibility(View.GONE);
//            holder.progressBar.setVisibility(View.GONE);
//        }
//
//
//        MyApplication.loadCategory(""+categoryId,holder.categoryTv);
//        MyApplication.loadPdfSize(
//                ""+pdfUrl,
//                ""+title,
//                holder.sizeTv
//        );
//
//        holder.itemView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(context, PdfDetailActivity.class);
//                intent.putExtra("bookId", bookId);
//                context.startActivity(intent);
//            }
//        });
//
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return pdfArrayList.size();
//    }
//
//    @Override
//    public Filter getFilter() {
//        if(filter == null){
//            filter = new FilterPdfUser(filterList, this);
//        }
//        return filter;
//    }
//
//    class HolderPdfUser extends RecyclerView.ViewHolder {
//        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
//        PDFView pdfView;
//        ProgressBar progressBar;
//
//        public HolderPdfUser(@NonNull View itemView) {
//            super(itemView);
//
//            // Gán biến binding (nếu chưa được gán trong adapter, cần truyền vào)
//            binding = RowPdfUserBinding.bind(itemView);
//
//            // Ánh xạ view từ binding
//            titleTv = binding.titleTv;
//            descriptionTv = binding.descriptionTv;
//            categoryTv = binding.categoryTv;
//            sizeTv = binding.sizeTv;
//            dateTv = binding.dateTv;
//            pdfView = binding.pdfView;
//            progressBar = binding.progressBar;
//        }
//    }
//
//}

public class AdapterPdfUser extends RecyclerView.Adapter<AdapterPdfUser.HolderPdfUser> implements Filterable {

    private Context context;
    public ArrayList<ModelPdf> pdfArrayList, filterList;
    private RowPdfUserBinding binding;
    private FilterPdfUser filter;

    public boolean isFiltering = false; // NEW: trạng thái lọc

    public AdapterPdfUser(Context context, ArrayList<ModelPdf> pdfArrayList) {
        this.context = context;
        this.pdfArrayList = pdfArrayList;
        this.filterList = pdfArrayList;
    }

    @NonNull
    @Override
    public HolderPdfUser onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        binding = RowPdfUserBinding.inflate(LayoutInflater.from(context), parent, false);
        return new HolderPdfUser(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull AdapterPdfUser.HolderPdfUser holder, int position) {
        ModelPdf model = pdfArrayList.get(position);
        String bookId = model.getId();
        String title = model.getTitle();
        String description = model.getDescription();
        String pdfUrl = model.getUrl();
        String categoryId = model.getCategoryId();
        long timestamp = model.getTimestamp();

        String date = MyApplication.formatTimestamp(timestamp);

        holder.titleTv.setText(title);
        holder.descriptionTv.setText(description);
        holder.dateTv.setText(date);

        holder.pdfView.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.GONE);

        // Chỉ load preview nếu không lọc
        if (!isFiltering) {
            holder.pdfView.setVisibility(View.VISIBLE);
            holder.progressBar.setVisibility(View.VISIBLE);

            MyApplication.loadPdfFromUrlSinglePage(
                    pdfUrl,
                    title,
                    holder.pdfView,
                    holder.progressBar,
                    null
            );
        }else {

            holder.pdfView.setVisibility(View.GONE);
            holder.progressBar.setVisibility(View.GONE);
        }

        MyApplication.loadCategory(categoryId, holder.categoryTv);
        MyApplication.loadPdfSize(pdfUrl, title, holder.sizeTv);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PdfDetailActivity.class);
            intent.putExtra("bookId", bookId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return pdfArrayList.size();
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new FilterPdfUser(filterList, this);
        }
        return filter;
    }

    class HolderPdfUser extends RecyclerView.ViewHolder {
        TextView titleTv, descriptionTv, categoryTv, sizeTv, dateTv;
        PDFView pdfView;
        ProgressBar progressBar;

        public HolderPdfUser(@NonNull View itemView) {
            super(itemView);
            binding = RowPdfUserBinding.bind(itemView);

            titleTv = binding.titleTv;
            descriptionTv = binding.descriptionTv;
            categoryTv = binding.categoryTv;
            sizeTv = binding.sizeTv;
            dateTv = binding.dateTv;
            pdfView = binding.pdfView;
            progressBar = binding.progressBar;
        }
    }
}

