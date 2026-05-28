package com.example.newjavaproject.nutrition;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.newjavaproject.R;

public class NutritionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nutrition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    EditText etW = view.findViewById(R.id.et_weight);
    EditText etH = view.findViewById(R.id.et_height);
    Button btnCalc = view.findViewById(R.id.btn_calculate);

    btnCalc.setOnClickListener(v -> {
    try {
        // 1. 取得性別
        RadioGroup rgGender = view.findViewById(R.id.rg_gender);
        boolean isMale = (rgGender.getCheckedRadioButtonId() == R.id.rb_male);

        double w = Double.parseDouble(etW.getText().toString().trim());
        double h = Double.parseDouble(etH.getText().toString().trim());

        // 計算 BMI 與熱量
        double bmi = w / Math.pow(h / 100.0, 2);
        // 男生公式: (10w + 6.25h - 5) * 1.375 | 女生公式: (10w + 6.25h - 161) * 1.375
        double tdee = ((10 * w) + (6.25 * h) + (isMale ? 5 : -161)) * 1.375;

        // 2. 根據性別與 BMI 判斷建議
        String status = (bmi < 18.5) ? "體重過輕" : (bmi < 24) ? "體重正常" : (bmi < 27) ? "過重" : "肥胖";
        
        String advice;
        if (isMale) {
            advice = (bmi < 18.5) ? "男性：建議增加重量訓練與蛋白質攝取。" : 
                     (bmi < 24) ? "男性：身體素質良好，保持運動習慣。" : 
                     (bmi < 27) ? "男性：稍微減少熱量攝取，建議多做有氧。" : "男性：建議諮詢營養師規劃減脂計畫。";
        } else {
            advice = (bmi < 18.5) ? "女性：建議增加營養密度高的食物攝取。" : 
                     (bmi < 24) ? "女性：體態標準，請維持目前的均衡飲食。" : 
                     (bmi < 27) ? "女性：建議調整飲食比例，減少精緻澱粉。" : "女性：體脂過高，建議諮詢專業規劃健康飲食。";
        }

        // 3. 更新 UI
        TextView tvCalories = view.findViewById(R.id.tv_total_calories);
        TextView tvBmiResult = view.findViewById(R.id.tv_bmi_result);
        TextView tvBmiAdvice = view.findViewById(R.id.tv_bmi_advice_text);
        
        tvCalories.setText(String.format("🔥 建議每日總熱量\n%.0f kcal", tdee));
        tvBmiResult.setText(String.format("性別: %s | BMI: %.1f (%s)", (isMale ? "男" : "女"), bmi, status));
        tvBmiAdvice.setText(advice);

        // 更新營養素卡片
        ((TextView)view.findViewById(R.id.tv_carbs)).setText(String.format("🍚攝取澱粉\n%.1f g", (tdee * 0.4) / 4));
        ((TextView)view.findViewById(R.id.tv_protein)).setText(String.format("💪蛋白質\n%.1f g", (tdee * 0.3) / 4));
        ((TextView)view.findViewById(R.id.tv_fat)).setText(String.format("🥑脂肪\n%.1f g", (tdee * 0.3) / 9));

        // 顯示卡片
        view.findViewById(R.id.card_calories).setVisibility(View.VISIBLE);
        view.findViewById(R.id.card_bmi_advice).setVisibility(View.VISIBLE);
        view.findViewById(R.id.layout_nutrients).setVisibility(View.VISIBLE);
        // 4. 動態顯示對應的體脂對照表圖片
        ImageView ivChart = view.findViewById(R.id.iv_body_fat_chart);

        // 根據性別切換圖片
        if (isMale) {
            ivChart.setImageResource(R.drawable.chart_male);
        } else {
            ivChart.setImageResource(R.drawable.chart_female);
        }

        // 設定顯示圖片
        ivChart.setVisibility(View.VISIBLE);
        // 顯示圖片
    } catch (Exception e) {
        Toast.makeText(getContext(), "請輸入數字", Toast.LENGTH_SHORT).show();
    }
});
}
}