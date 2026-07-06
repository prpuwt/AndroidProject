package com.example.mybill;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mybill.db.BillDatabaseHelper;
import com.example.mybill.model.Bill;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrashActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private RecyclerView rvTrash;
    private View layoutEmpty;
    private TextView tvCount;
    private CheckBox cbSelectAll;
    private View layoutBottomActions;
    private MaterialButton btnRestore, btnBack;
    private TrashAdapter adapter;
    private List<Bill> deletedBills;

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(Locale.SIMPLIFIED_CHINESE);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_trash);
        overridePendingTransition(R.anim.slide_in_up, 0);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_trash), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new BillDatabaseHelper(this);
        rvTrash = findViewById(R.id.rv_trash);
        layoutEmpty = findViewById(R.id.layout_empty);
        tvCount = findViewById(R.id.tv_trash_count);
        cbSelectAll = findViewById(R.id.cb_select_all);
        layoutBottomActions = findViewById(R.id.layout_bottom_actions);
        btnRestore = findViewById(R.id.btn_restore);
        MaterialButton btnDelete = findViewById(R.id.btn_delete);
        btnBack = findViewById(R.id.btn_back);

        rvTrash.setLayoutManager(new LinearLayoutManager(this));

        btnBack.setOnClickListener(v -> finish());
        loadData();

        cbSelectAll.setOnCheckedChangeListener((v, checked) -> {
            if (adapter != null) adapter.selectAll(checked);
        });

        btnRestore.setOnClickListener(v -> restoreSelected());
        btnDelete.setOnClickListener(v -> deleteSelected());
    }

    private void loadData() {
        deletedBills = dbHelper.getDeletedBills();
        if (deletedBills.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTrash.setVisibility(View.GONE);
            layoutBottomActions.setVisibility(View.GONE);
            tvCount.setVisibility(View.GONE);
            return;
        }
        layoutEmpty.setVisibility(View.GONE);
        rvTrash.setVisibility(View.VISIBLE);
        tvCount.setVisibility(View.VISIBLE);
        tvCount.setText(deletedBills.size() + "条记录");
        layoutBottomActions.setVisibility(View.VISIBLE);

        adapter = new TrashAdapter(deletedBills);
        rvTrash.setAdapter(adapter);
    }

    private void restoreSelected() {
        if (adapter == null) return;
        List<Integer> selected = adapter.getSelectedPositions();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先勾选要恢复的记录", Toast.LENGTH_SHORT).show();
            return;
        }
        int restored = 0;
        for (int i = selected.size() - 1; i >= 0; i--) {
            int pos = selected.get(i);
            dbHelper.restoreBill(deletedBills.get(pos).getId());
            deletedBills.remove(pos);
            restored++;
        }
        adapter.notifyDataSetChanged();
        updateAfterOperation();
        Toast.makeText(this, "已恢复" + restored + "条记录", Toast.LENGTH_SHORT).show();
    }

    private void deleteSelected() {
        if (adapter == null) return;
        List<Integer> selected = adapter.getSelectedPositions();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先勾选要删除的记录", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage("确定要永久删除选中的 " + selected.size() + " 条记录吗？此操作不可恢复。")
                .setPositiveButton("永久删除", (d, w) -> {
                    for (int i = selected.size() - 1; i >= 0; i--) {
                        int pos = selected.get(i);
                        dbHelper.permanentlyDeleteBill(deletedBills.get(pos).getId());
                        deletedBills.remove(pos);
                    }
                    adapter.notifyDataSetChanged();
                    updateAfterOperation();
                    Toast.makeText(this, "已删除" + selected.size() + "条记录",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateAfterOperation() {
        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(false);
        cbSelectAll.setOnCheckedChangeListener((v, checked) -> {
            if (adapter != null) adapter.selectAll(checked);
        });

        if (deletedBills.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvTrash.setVisibility(View.GONE);
            layoutBottomActions.setVisibility(View.GONE);
            tvCount.setVisibility(View.GONE);
        } else {
            tvCount.setText(deletedBills.size() + "条记录");
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    // ==================== Adapter ====================

    private class TrashAdapter extends RecyclerView.Adapter<TrashAdapter.ViewHolder> {

        private final List<Bill> bills;
        private final SparseBooleanArray selectedItems = new SparseBooleanArray();

        TrashAdapter(List<Bill> bills) {
            this.bills = bills;
        }

        void selectAll(boolean select) {
            selectedItems.clear();
            if (select) {
                for (int i = 0; i < bills.size(); i++) {
                    selectedItems.put(i, true);
                }
            }
            notifyDataSetChanged();
        }

        List<Integer> getSelectedPositions() {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < bills.size(); i++) {
                if (selectedItems.get(i, false)) list.add(i);
            }
            return list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_trash_bill, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Bill bill = bills.get(position);
            String date = bill.getDate();
            if (date.length() >= 10) date = date.substring(5);
            holder.tvDate.setText(date);
            holder.tvDesc.setText(bill.getLocation() + "  " + bill.getDescription());
            holder.tvPerson.setText(bill.getPersonName());
            holder.tvAmount.setText(String.format(Locale.getDefault(), "¥%.2f", bill.getAmount()));
            holder.cb.setChecked(selectedItems.get(position, false));

            holder.itemView.setOnClickListener(v -> {
                holder.cb.toggle();
                if (holder.cb.isChecked()) {
                    selectedItems.put(position, true);
                } else {
                    selectedItems.delete(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return bills.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cb;
            TextView tvDate, tvDesc, tvPerson, tvAmount;

            ViewHolder(View itemView) {
                super(itemView);
                cb = itemView.findViewById(R.id.cb_trash_item);
                tvDate = itemView.findViewById(R.id.tv_trash_date);
                tvDesc = itemView.findViewById(R.id.tv_trash_desc);
                tvPerson = itemView.findViewById(R.id.tv_trash_person);
                tvAmount = itemView.findViewById(R.id.tv_trash_amount);
            }
        }
    }
}
