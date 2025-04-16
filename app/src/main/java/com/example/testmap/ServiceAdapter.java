package com.example.testmap;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {
    private List<String> serviceList;
    private OnItemClickListener listener;

    // Interface để xử lý sự kiện nhấn vào mũi tên
    public interface OnItemClickListener {
        void onArrowClick(String service);
    }

    // Constructor với listener
    public ServiceAdapter(List<String> serviceList, OnItemClickListener listener) {
        this.serviceList = serviceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        String service = serviceList.get(position);
        holder.tvServiceName.setText(service);

        // Xử lý sự kiện nhấn vào mũi tên
        holder.ivArrow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onArrowClick(service);
            }
        });

        // Xử lý sự kiện nhấn vào toàn bộ item (giữ nguyên nếu cần)
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(holder.itemView.getContext(), "Đã chọn: " + service, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName;
        ImageView ivArrow;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
        }
    }
}