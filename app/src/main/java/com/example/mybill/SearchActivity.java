package com.example.mybill;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mybill.db.BillDatabaseHelper;
import com.example.mybill.model.Bill;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private BillDatabaseHelper dbHelper;
    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private TextView tvHint, tvNoResults;
    private ResultsAdapter adapter;

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
        setContentView(R.layout.activity_search);
        overridePendingTransition(R.anim.slide_in_up, 0);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_search), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new BillDatabaseHelper(this);
        etSearch = findViewById(R.id.et_search);
        rvResults = findViewById(R.id.rv_results);
        tvHint = findViewById(R.id.tv_hint);
        tvNoResults = findViewById(R.id.tv_no_results);
        MaterialButton btnBack = findViewById(R.id.btn_back);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        btnBack.setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                performSearch(s.toString().trim());
            }
        });

        // Auto-focus search field
        etSearch.requestFocus();
    }

    private void performSearch(String keyword) {
        if (keyword.isEmpty()) {
            tvHint.setVisibility(View.VISIBLE);
            tvNoResults.setVisibility(View.GONE);
            rvResults.setVisibility(View.GONE);
            return;
        }

        List<Bill> results = dbHelper.searchBills(keyword);
        tvHint.setVisibility(View.GONE);

        if (results.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            rvResults.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            rvResults.setVisibility(View.VISIBLE);
            adapter = new ResultsAdapter(results);
            rvResults.setAdapter(adapter);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    // ==================== Adapter ====================

    private class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

        private final List<Bill> bills;

        ResultsAdapter(List<Bill> bills) {
            this.bills = bills;
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
            holder.cb.setVisibility(View.GONE);
            holder.tvDate.setText(date);
            holder.tvDesc.setText(bill.getLocation() + "  " + bill.getDescription());
            holder.tvPerson.setText(bill.getPersonName());
            holder.tvAmount.setText(String.format(Locale.getDefault(), "¥%.2f", bill.getAmount()));
            holder.tvAmount.setTextColor(
                    holder.itemView.getContext().getResources().getColor(R.color.amount_green));

            holder.itemView.setOnClickListener(v -> {
                try {
                    String[] parts = bill.getDate().split("-");
                    Intent intent = new Intent(SearchActivity.this, MainActivity.class);
                    intent.putExtra("year", Integer.parseInt(parts[0]));
                    intent.putExtra("month", Integer.parseInt(parts[1]));
                    intent.putExtra("day", Integer.parseInt(parts[2]));
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(SearchActivity.this, "无法跳转到该账单",
                            Toast.LENGTH_SHORT).show();
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
