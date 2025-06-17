package com.example.bookapp.filters;

import android.widget.Filter;

import com.example.bookapp.adapters.AdapterCategory;
import com.example.bookapp.adapters.AdapterPdfAdmin;
import com.example.bookapp.models.ModelCategory;
import com.example.bookapp.models.ModelPdf;

import java.util.ArrayList;

public class FilterPdfAdmin extends Filter {

    // Danh sách gốc cần lọc
    ArrayList<ModelPdf> filterList;

    // Adapter cần cập nhật dữ liệu sau khi lọc
    AdapterPdfAdmin adapterPdfAdmin;

    // Constructor
    public FilterPdfAdmin(ArrayList<ModelPdf> filterList, AdapterPdfAdmin adapterPdfAdmin) {
        this.filterList = filterList;
        this.adapterPdfAdmin = adapterPdfAdmin;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();

        // value should not be null and empty
        if (constraint != null && constraint.length() > 0) {

            // change to upper case, or lower case to avoid case sensitivity
            constraint = constraint.toString().toUpperCase();

            ArrayList<ModelPdf> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++) {
                // validate
                if (filterList.get(i).getTitle().toUpperCase().contains(constraint)) {
                    // add to filtered list
                    filteredModels.add(filterList.get(i));
                }
            }

            results.count = filteredModels.size();
            results.values = filteredModels;
        } else {
            results.count = filterList.size();
            results.values = filterList;
        }

        return results;
    }


    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        // Cập nhật lại dữ liệu hiển thị
        adapterPdfAdmin.pdfArrayList = (ArrayList<ModelPdf>) results.values;
        adapterPdfAdmin.notifyDataSetChanged();
    }


}
