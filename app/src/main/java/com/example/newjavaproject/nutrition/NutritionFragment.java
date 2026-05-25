package com.example.newjavaproject.nutrition;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.newjavaproject.R;
import com.example.newjavaproject.nutrition.adapter.NutritionAdapter;
import java.util.ArrayList;
import java.util.List;

public class NutritionFragment extends Fragment {

    private String selectedGender = "男";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nutrition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化元件
        RecyclerView recyclerView = view.findViewById(R.id.recycler_nutrition);
        Button btnWater = view.findViewById(R.id.btn_set_water_reminder);
        EditText etW = view.findViewById(R.id.et_weight);
        EditText etH = view.findViewById(R.id.et_height);
        Button btnCalc = view.findViewById(R.id.btn_calculate);
        ProgressBar pbBmi = view.findViewById(R.id.pb_bmi);
        TextView tvBmi = view.findViewById(R.id.tv_bmi_result);

        // RecyclerView 設定
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<NutritionItem> list = new ArrayList<>();
        list.add(new NutritionItem("🏃‍♂️ 運動補給", "運動前補充碳水，運動後蛋白質。", android.R.drawable.ic_media_play));
        list.add(new NutritionItem("💧 飲水建議", "每 30 分鐘補充水分。", android.R.drawable.ic_menu_info_details));
        recyclerView.setAdapter(new NutritionAdapter(list));

        // 飲水提醒 (15-300 分鐘)
        btnWater.setOnClickListener(v -> showTimePickerDialog());

        // 計算邏輯
        btnCalc.setOnClickListener(v -> showGenderDialog(etW, etH, pbBmi, tvBmi));
    }

    private void showGenderDialog(EditText etW, EditText etH, ProgressBar pb, TextView tv) {
        String[] genders = {"男", "女"};
        new AlertDialog.Builder(requireContext())
            .setTitle("選擇性別")
            .setSingleChoiceItems(genders, 0, (d, w) -> selectedGender = genders[w])
            .setPositiveButton("計算", (d, w) -> performCalculation(etW, etH, pb, tv))
            .show();
    }

    private void performCalculation(EditText etW, EditText etH, ProgressBar pb, TextView tv) {
        try {
            double w = Double.parseDouble(etW.getText().toString());
            double h = Double.parseDouble(etH.getText().toString());
            double bmi = w / ((h/100.0) * (h/100.0));
            double tdee = ((10 * w) + (6.25 * h) - (selectedGender.equals("男") ? 5 : 161)) * 1.2;

            // 更新 UI
            pb.setProgress((int) Math.min(bmi, 35));
            int color = (bmi < 18.5) ? Color.BLUE : (bmi < 24) ? Color.parseColor("#27AE60") : (bmi < 27) ? Color.parseColor("#F39C12") : Color.RED;
            String status = (bmi < 18.5) ? "過輕" : (bmi < 24) ? "適中" : (bmi < 27) ? "過重" : "肥胖";

            pb.setProgressTintList(ColorStateList.valueOf(color));
            tv.setText(String.format("BMI: %.1f (%s)", bmi, status));
            tv.setTextColor(color);

            new AlertDialog.Builder(requireContext())
                .setTitle("營養攝取建議")
                .setMessage(String.format("每日總熱量: %.0f kcal\n\n🍚 澱粉: %.1f g\n💪 蛋白: %.1f g\n🥑 脂肪: %.1f g\n💧 水分: %.1f L", 
                            tdee, (tdee*0.4)/4, (tdee*0.3)/4, (tdee*0.3)/9, w*0.035))
                .setPositiveButton("知道了", null).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "請輸入數字", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePickerDialog() {
        final NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(15); picker.setMaxValue(300); picker.setValue(60);
        new AlertDialog.Builder(requireContext())
            .setTitle("提醒間隔 (15-300 分鐘)")
            .setView(picker)
            .setPositiveButton("確認", (d, w) -> Toast.makeText(getContext(), "已設定 " + picker.getValue() + " 分鐘", Toast.LENGTH_SHORT).show())
            .show();
    }
}