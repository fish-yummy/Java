package com.example.newjavaproject.nutrition.adapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newjavaproject.R;
import com.example.newjavaproject.nutrition.service.NutritionItem;
import java.util.List;

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.NutritionViewHolder> {

    private List<NutritionItem> nutritionList; 

    public NutritionAdapter(List<NutritionItem> nutritionList) {
        this.nutritionList = nutritionList;
    }

    @NonNull
    @Override
    public NutritionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nutrition, parent, false);
        return new NutritionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutritionViewHolder holder, int position) {
        NutritionItem currentItem = nutritionList.get(position);
        
        holder.tvTitle.setText(currentItem.getTitle());
        holder.tvDescription.setText(currentItem.getDescription());
        holder.imgFood.setImageResource(currentItem.getImageResId());

        // --- 核心功能：設定點擊行為 ---
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            handleCardClick(context, position, currentItem.getTitle());
        });
    }

    // 根據點擊的位置或標題，執行不同的功能
    private void handleCardClick(Context context, int position, String title) {
        String message;
        switch (position) {
            case 0:
                message = "運動前 30 分鐘補充碳水化合物（如香蕉、燕麥），能提供您持續的體力，避免運動中疲勞。";
                break;
            case 1:
                message = "運動後建議補充優質蛋白質（如豆漿、水煮蛋），這是修復肌肉、維持肌力的關鍵。";
                break;
            case 2:
                message = "運動中記得每 30 分鐘小口補充水分，避免脫水造成的血壓波動。";
                break;
            default:
                message = "保持規律飲食，讓運動成效更顯著！";
        }

        new androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("知道了", (dialog, which) -> dialog.dismiss())
            .show();
    }

    @Override
    public int getItemCount() {
        return nutritionList != null ? nutritionList.size() : 0;
    }

    static class NutritionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDescription;
        ImageView imgFood;

        public NutritionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_food_title);
            tvDescription = itemView.findViewById(R.id.tv_food_desc);
            imgFood = itemView.findViewById(R.id.img_food);
        }
    }
}