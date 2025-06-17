package com.example.bookapp.filters;

import android.widget.Filter;

import com.example.bookapp.adapters.AdapterCategory;
import com.example.bookapp.models.ModelCategory;

import java.util.ArrayList;

public class FilterCategory extends Filter {

    // Danh sách gốc cần lọc
    ArrayList<ModelCategory> filterList;

    // Adapter cần cập nhật dữ liệu sau khi lọc
    AdapterCategory adapterCategory;

    // Constructor
    public FilterCategory(ArrayList<ModelCategory> filterList, AdapterCategory adapterCategory) {
        this.filterList = filterList;
        this.adapterCategory = adapterCategory;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();

        // value should not be null and empty
        if (constraint != null && constraint.length() > 0) {

            // change to upper case, or lower case to avoid case sensitivity
            constraint = constraint.toString().toUpperCase();

            ArrayList<ModelCategory> filteredModels = new ArrayList<>();

            for (int i = 0; i < filterList.size(); i++) {
                // validate
                if (filterList.get(i).getCategory().toUpperCase().contains(constraint)) {
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
        adapterCategory.categoryArrayList = (ArrayList<ModelCategory>) results.values;
        adapterCategory.notifyDataSetChanged();
    }


}
