package com.example.mybill.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mybill.R;
import com.example.mybill.model.Bill;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class BillAdapter extends RecyclerView.Adapter<BillAdapter.ViewHolder> {

    public interface OnBillClickListener {
        void onBillClick(Bill bill);
        void onBillLongClick(View itemView, Bill bill);
    }

    private final List<Bill> bills;
    private OnBillClickListener listener;

    public BillAdapter(List<Bill> bills) {
        this.bills = bills;
    }

    public void setOnBillClickListener(OnBillClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bill bill = bills.get(position);
        String date = bill.getDate();
        if (date.length() >= 10) {
            holder.tvDate.setText(date.substring(5));
        } else {
            holder.tvDate.setText(date);
        }
        holder.tvPerson.setText(bill.getPersonName());
        holder.tvLocation.setText(bill.getLocation());
        holder.tvDescription.setText(bill.getDescription());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "¥%.2f", bill.getAmount()));

        if (bill.isSettled()) {
            holder.cardBill.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.settled_bg));
            holder.cardBill.setStrokeWidth(1);
            holder.cardBill.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.card_stroke));
            int settledTextColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.settled_text);
            holder.tvDate.setTextColor(settledTextColor);
            holder.tvLocation.setTextColor(settledTextColor);
            holder.tvDescription.setTextColor(settledTextColor);
            holder.tvPerson.setTextColor(settledTextColor);
            holder.tvAmount.setTextColor(settledTextColor);
            holder.tvSettledWatermark.setVisibility(View.VISIBLE);
        } else {
            int cardBgColor = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white);
            holder.cardBill.setCardBackgroundColor(cardBgColor);
            holder.cardBill.setStrokeWidth(0);
            holder.tvDate.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_hint));
            holder.tvLocation.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
            holder.tvDescription.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.tvPerson.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_blue));
            holder.tvAmount.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.amount_green));
            holder.tvSettledWatermark.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onBillClick(bill);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onBillLongClick(holder.itemView, bill);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardBill;
        TextView tvDate, tvLocation, tvPerson, tvDescription, tvAmount, tvSettledWatermark;

        ViewHolder(View itemView) {
            super(itemView);
            cardBill = itemView.findViewById(R.id.card_bill);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvPerson = itemView.findViewById(R.id.tv_person);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvSettledWatermark = itemView.findViewById(R.id.tv_settled_watermark);
        }
    }
}
