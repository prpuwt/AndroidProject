package com.example.mybill.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mybill.R;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageHolder> {

    private static final int[][] PAGE_DATA = {
        // { titleRes, descRes, iconType, iconRes }
        // iconType: 0=image (bill.jpg), 1=text
    };

    @NonNull
    @Override
    public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_page, parent, false);
        return new PageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageHolder holder, int position) {
        switch (position) {
            case 0:
                holder.ivImage.setVisibility(View.VISIBLE);
                holder.tvIcon.setVisibility(View.GONE);
                holder.ivImage.setImageResource(R.drawable.bill);
                holder.tvTitle.setText("欢迎使用");
                holder.tvDesc.setText("记录每位工人的工钱\n方便快捷对账");
                break;
            case 1:
                holder.ivImage.setVisibility(View.GONE);
                holder.tvIcon.setVisibility(View.VISIBLE);
                holder.tvIcon.setText("＋");
                holder.tvTitle.setText("添加记录");
                holder.tvDesc.setText("点击右上角 ＋ 号\n选择工人、填写金额、保存");
                break;
            case 2:
                holder.ivImage.setVisibility(View.GONE);
                holder.tvIcon.setVisibility(View.VISIBLE);
                holder.tvIcon.setText("📋");
                holder.tvTitle.setText("查看管理");
                holder.tvDesc.setText("左右滑动查看每日账单\n长按可编辑或删除记录");
                break;
            case 3:
                holder.ivImage.setVisibility(View.GONE);
                holder.tvIcon.setVisibility(View.VISIBLE);
                holder.tvIcon.setText("📊");
                holder.tvTitle.setText("数据统计");
                holder.tvDesc.setText("按日期范围和人员筛选\n支持导出和导入账单备份");
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    static class PageHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvIcon;
        TextView tvTitle;
        TextView tvDesc;

        PageHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_page_image);
            tvIcon = itemView.findViewById(R.id.tv_page_icon);
            tvTitle = itemView.findViewById(R.id.tv_page_title);
            tvDesc = itemView.findViewById(R.id.tv_page_desc);
        }
    }
}
