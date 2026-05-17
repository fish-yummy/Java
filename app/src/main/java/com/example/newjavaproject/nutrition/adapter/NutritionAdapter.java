package com.example.newjavaproject.nutrition.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newjavaproject.R;
import java.util.List;

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.NutritionViewHolder> {

    // TODO: 定義一個資料模型類別 (例如 NutritionItem)，並替換這裡的 String
    private List<String> nutritionList; 

    public NutritionAdapter(List<String> nutritionList) {
        this.nutritionList = nutritionList;
    }

    @NonNull
    @Override
    public NutritionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //TODO: 建立 item_nutrition.xml 作為單一卡片的佈局
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nutrition, parent, false);
        return new NutritionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutritionViewHolder holder, int position) {
        // TODO: 根據 position 拿出資料，設定給 holder 裡的 TextView 和 ImageView
        String currentItem = nutritionList.get(position);
        // holder.tvTitle.setText(currentItem);
    }

    @Override
    public int getItemCount() {
        return nutritionList != null ? nutritionList.size() : 0;
    }

    // 負責綁定單一卡片內的 UI 元件
    static class NutritionViewHolder extends RecyclerView.ViewHolder {
        // TextView tvTitle;
        // ImageView imgFood;

        public NutritionViewHolder(@NonNull View itemView) {
            super(itemView);
            // TODO: 使用 itemView.findViewById 綁定卡片內的元件
        }
    }
}