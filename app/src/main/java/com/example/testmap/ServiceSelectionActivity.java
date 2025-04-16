package com.example.testmap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ServiceSelectionActivity extends AppCompatActivity {

    private RecyclerView rvServiceList;
    private ServiceAdapter serviceAdapter;
    private List<String> serviceList;
    private TextView tvVehicleInfo;
    private TextView tvChangeVehicle;
    private ImageView ivClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_selection);

        // Khởi tạo các view
        tvVehicleInfo = findViewById(R.id.tv_vehicle_info);
        tvChangeVehicle = findViewById(R.id.tv_change_vehicle);
        ivClose = findViewById(R.id.iv_close);
        rvServiceList = findViewById(R.id.rv_service_list);
        rvServiceList.setLayoutManager(new LinearLayoutManager(this));

        // Tạo danh sách dịch vụ thực tế
        serviceList = new ArrayList<>();
        serviceList.add("Rửa xe");
        serviceList.add("Thay nhớt");
        serviceList.add("Nạp bình");
        serviceList.add("Bảo dưỡng");
        serviceList.add("Đổi lốp");
        serviceList.add("Sơn xe");
        serviceList.add("Kiểm tra động cơ");
        serviceList.add("Sửa chữa");

        // Thiết lập Adapter cho RecyclerView với listener
        serviceAdapter = new ServiceAdapter(serviceList, new ServiceAdapter.OnItemClickListener() {
            @Override
            public void onArrowClick(String service) {
                Intent intent = new Intent(ServiceSelectionActivity.this, ServiceDetailActivity.class);
                intent.putExtra("SERVICE_NAME", service);
                startActivity(intent);
            }
        });
        rvServiceList.setAdapter(serviceAdapter);

        // Xử lý sự kiện nhấn "Đổi phương tiện"
        tvChangeVehicle.setOnClickListener(v -> showVehicleInfoDialog());

        // Xử lý sự kiện nhấn nút đóng
        ivClose.setOnClickListener(v -> finish());
    }

    private void showVehicleInfoDialog() {
        // Tạo dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_vehicle_info, null);
        builder.setView(dialogView);

        // Khởi tạo các view trong dialog
        EditText etVehicleName = dialogView.findViewById(R.id.et_vehicle_name);
        EditText etVehicleYear = dialogView.findViewById(R.id.et_vehicle_year);
        Button btnBack = dialogView.findViewById(R.id.btn_back);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // Hiển thị dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Xử lý sự kiện nhấn nút "Quay lại"
        btnBack.setOnClickListener(v -> dialog.dismiss());

        // Xử lý sự kiện nhấn nút "Xác nhận"
        btnConfirm.setOnClickListener(v -> {
            String vehicleName = etVehicleName.getText().toString().trim();
            String vehicleYear = etVehicleYear.getText().toString().trim();

            // Kiểm tra dữ liệu nhập
            if (vehicleName.isEmpty() || vehicleYear.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin xe", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật thông tin xe trên giao diện
            tvVehicleInfo.setText(vehicleName + ", " + vehicleYear);
            dialog.dismiss();
        });
    }
}