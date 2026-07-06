package com.example.mybill;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import androidx.viewpager2.widget.ViewPager2;

import com.example.mybill.adapter.BillAdapter;
import com.example.mybill.db.BillDatabaseHelper;
import com.example.mybill.model.Bill;
import com.example.mybill.view.PatternLockView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.example.mybill.CoachMarkOverlay.CoachMarkStep;
import com.example.mybill.config.Constants;
import com.example.mybill.util.CryptoUtils;
import com.example.mybill.util.DisplayUtils;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = Constants.PREFS_NAME;
    private static final String KEY_HAS_SEEN_MAIN_GUIDE = Constants.KEY_HAS_SEEN_MAIN_GUIDE;
    private static final String KEY_SHOW_SETTLED = "show_settled";

    private ViewPager2 viewPager;
    private TextView tvYearMonth;
    private HorizontalScrollView hsvDays;
    private ChipGroup chipGroupDays;
    private BillDatabaseHelper dbHelper;

    private int selectedYear;
    private int selectedMonth;
    private int selectedDay = 0;
    private int maxDay;
    private final List<Chip> dayChips = new ArrayList<>();
    private BillPagerAdapter pagerAdapter;
    private boolean isSyncing = false;
    private boolean showSettled = false;

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
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new BillDatabaseHelper(this);

        viewPager = findViewById(R.id.view_pager);
        tvYearMonth = findViewById(R.id.tv_year_month);
        hsvDays = findViewById(R.id.hsv_days);
        chipGroupDays = findViewById(R.id.chip_group_days);
        MaterialButton btnMenu = findViewById(R.id.btn_menu);
        MaterialButton btnSearch = findViewById(R.id.btn_search);
        MaterialButton btnAdd = findViewById(R.id.btn_add);
        MaterialButton btnStats = findViewById(R.id.btn_stats);
        MaterialButton btnPrevYear = findViewById(R.id.btn_prev_year);
        MaterialButton btnPrevMonth = findViewById(R.id.btn_prev_month);
        MaterialButton btnNextMonth = findViewById(R.id.btn_next_month);
        MaterialButton btnNextYear = findViewById(R.id.btn_next_year);

        Calendar now = Calendar.getInstance();
        selectedYear = now.get(Calendar.YEAR);
        selectedMonth = now.get(Calendar.MONTH) + 1;

        pagerAdapter = new BillPagerAdapter();
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(Math.max(0.75f, 1f - absPos * 0.5f));
            page.setScaleY(Math.max(0.92f, 1f - absPos * 0.12f));
            page.setScaleX(Math.max(0.95f, 1f - absPos * 0.08f));
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (!isSyncing) {
                    selectedDay = position;
                    selectDayChip(position);
                }
            }
        });

        initDayChips();

        btnPrevYear.setOnClickListener(v -> {
            selectedYear--;
            selectedDay = 0;
            navigateToMonth();
        });
        btnNextYear.setOnClickListener(v -> {
            selectedYear++;
            selectedDay = 0;
            navigateToMonth();
        });
        btnPrevMonth.setOnClickListener(v -> {
            selectedMonth--;
            if (selectedMonth < 1) { selectedMonth = 12; selectedYear--; }
            selectedDay = 0;
            navigateToMonth();
        });
        btnNextMonth.setOnClickListener(v -> {
            selectedMonth++;
            if (selectedMonth > 12) { selectedMonth = 1; selectedYear++; }
            selectedDay = 0;
            navigateToMonth();
        });

        tvYearMonth.setOnClickListener(v -> showMonthPickerDialog());

        MaterialButton btnToday = findViewById(R.id.btn_today);
        btnToday.setOnClickListener(v -> {
            Calendar today = Calendar.getInstance();
            selectedYear = today.get(Calendar.YEAR);
            selectedMonth = today.get(Calendar.MONTH) + 1;
            selectedDay = today.get(Calendar.DAY_OF_MONTH);
            navigateToMonth();
        });

        btnAdd.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddBillActivity.class)));
        btnStats.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StatisticsActivity.class)));
        btnMenu.setOnClickListener(v -> showMainMenu());
        btnSearch.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        handleSearchNavigation();

        showSettled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_SHOW_SETTLED, false);

        navigateToMonth();

        // 首次进入 MainActivity 自动展示使用引导
        if (!getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_HAS_SEEN_MAIN_GUIDE, false)) {
            findViewById(R.id.main).postDelayed(this::startCoachMarkGuide, 600);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSearchNavigation();
    }

    private void handleSearchNavigation() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("year")) {
            selectedYear = intent.getIntExtra("year", selectedYear);
            selectedMonth = intent.getIntExtra("month", selectedMonth);
            selectedDay = intent.getIntExtra("day", 0);
            intent.removeExtra("year");
            intent.removeExtra("month");
            intent.removeExtra("day");
            navigateToMonth();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    private void refreshUI() {
        initDayChips();
        pagerAdapter.notifyDataSetChanged();
    }

    private void navigateToMonth() {
        updateYearMonthDisplay();
        initDayChips();
        pagerAdapter.notifyDataSetChanged();
        isSyncing = true;
        viewPager.setCurrentItem(selectedDay, false);
        isSyncing = false;
    }

    // ==================== 日期 Chip ====================

    private void initDayChips() {
        chipGroupDays.removeAllViews();
        dayChips.clear();

        // 一次性查询当月所有账单，按天计数
        List<Bill> monthBills = dbHelper.getBillsByMonth(selectedYear, selectedMonth);
        int[] counts = new int[32]; // 1-based day indexing
        for (Bill b : monthBills) {
            try {
                String date = b.getDate();
                if (date != null && date.length() >= 10) {
                    int d = Integer.parseInt(date.substring(8, 10));
                    if (d >= 1 && d <= 31) counts[d]++;
                }
            } catch (Exception ignored) {}
        }

        Chip chipAll = new Chip(this);
        chipAll.setText("全部");
        chipAll.setCheckable(true);
        chipAll.setOnClickListener(v -> {
            if (isSyncing) return;
            v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                    .start();
            selectedDay = 0;
            selectDayChip(0);
            isSyncing = true;
            viewPager.setCurrentItem(0, true);
            isSyncing = false;
        });
        chipGroupDays.addView(chipAll);
        dayChips.add(chipAll);

        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth - 1, 1);
        maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 1; i <= maxDay; i++) {
            Chip chip = new Chip(this);
            if (counts[i] > 0) {
                String text = i + "日(" + counts[i] + ")";
                SpannableString sp = new SpannableString(text);
                int badgeStart = text.indexOf('(');
                sp.setSpan(new ForegroundColorSpan(0xFFFF8F00), badgeStart, text.length(), 0);
                chip.setText(sp);
            } else {
                chip.setText(i + "日");
            }
            chip.setCheckable(true);
            final int day = i;
            chip.setOnClickListener(v -> {
                if (isSyncing) return;
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(80)
                        .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                        .start();
                selectedDay = day;
                selectDayChip(day);
                isSyncing = true;
                viewPager.setCurrentItem(day, true);
                isSyncing = false;
            });
            chipGroupDays.addView(chip);
            dayChips.add(chip);
        }

        if (selectedDay > maxDay) selectedDay = 0;
        selectDayChip(selectedDay);
    }

    private void selectDayChip(int day) {
        for (int i = 0; i < dayChips.size(); i++) {
            dayChips.get(i).setChecked(i == day);
        }
        if (day >= 0 && day < dayChips.size()) {
            Chip selected = dayChips.get(day);
            selected.post(() -> {
                int scrollX = selected.getLeft() - (hsvDays.getWidth() - selected.getWidth()) / 2;
                if (scrollX < 0) scrollX = 0;
                hsvDays.smoothScrollTo(scrollX, 0);
            });
        }
    }

    private void updateYearMonthDisplay() {
        tvYearMonth.setText(String.format(Locale.getDefault(), "%d年%d月", selectedYear, selectedMonth));
    }

    private void showMonthPickerDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 8);

        // 年份选择
        LinearLayout yearRow = new LinearLayout(this);
        yearRow.setOrientation(LinearLayout.HORIZONTAL);
        yearRow.setGravity(android.view.Gravity.CENTER);

        MaterialButton btnYearMinus = new MaterialButton(this, null,
                com.google.android.material.R.attr.borderlessButtonStyle);
        btnYearMinus.setText("−");
        btnYearMinus.setTextSize(24);
        btnYearMinus.setTextColor(getResources().getColor(R.color.primary_teal, null));

        EditText etYear = new EditText(this);
        etYear.setText(String.valueOf(selectedYear));
        etYear.setTextSize(22);
        etYear.setTextColor(getResources().getColor(R.color.text_primary, null));
        etYear.setGravity(android.view.Gravity.CENTER);
        etYear.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etYear.setSingleLine();
        LinearLayout.LayoutParams yearParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        yearParams.setMargins(16, 0, 16, 0);
        etYear.setLayoutParams(yearParams);

        MaterialButton btnYearPlus = new MaterialButton(this, null,
                com.google.android.material.R.attr.borderlessButtonStyle);
        btnYearPlus.setText("+");
        btnYearPlus.setTextSize(24);
        btnYearPlus.setTextColor(getResources().getColor(R.color.primary_teal, null));

        btnYearMinus.setOnClickListener(v -> {
            int y = Integer.parseInt(etYear.getText().toString());
            if (y > 2000) etYear.setText(String.valueOf(y - 1));
        });
        btnYearPlus.setOnClickListener(v -> {
            int y = Integer.parseInt(etYear.getText().toString());
            if (y < 2100) etYear.setText(String.valueOf(y + 1));
        });

        yearRow.addView(btnYearMinus);
        yearRow.addView(etYear);
        yearRow.addView(btnYearPlus);
        root.addView(yearRow);

        // 月份网格
        LinearLayout monthGrid = new LinearLayout(this);
        monthGrid.setOrientation(LinearLayout.VERTICAL);
        monthGrid.setPadding(0, 20, 0, 8);
        root.addView(monthGrid);

        final int[] selectedMonthInDialog = {selectedMonth};

        for (int row = 0; row < 3; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(android.view.Gravity.CENTER);
            for (int col = 0; col < 4; col++) {
                int month = row * 4 + col + 1;
                MaterialButton btnMonth = new MaterialButton(this, null,
                        com.google.android.material.R.attr.borderlessButtonStyle);
                btnMonth.setText(month + "月");
                btnMonth.setTextSize(15);
                btnMonth.setCornerRadius(20);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0,
                        DisplayUtils.dpToPx(44), 1);
                btnParams.setMargins(4, 4, 4, 4);
                btnMonth.setLayoutParams(btnParams);
                updateMonthButtonStyle(btnMonth, month == selectedMonthInDialog[0]);
                final int m = month;
                btnMonth.setOnClickListener(v -> {
                    if (selectedMonthInDialog[0] != m) {
                        selectedMonthInDialog[0] = m;
                        for (int i = 0; i < monthGrid.getChildCount(); i++) {
                            LinearLayout rl = (LinearLayout) monthGrid.getChildAt(i);
                            for (int j = 0; j < rl.getChildCount(); j++) {
                                MaterialButton mb = (MaterialButton) rl.getChildAt(j);
                                int bMonth = i * 4 + j + 1;
                                updateMonthButtonStyle(mb, bMonth == m);
                            }
                        }
                    }
                });
                rowLayout.addView(btnMonth);
            }
            monthGrid.addView(rowLayout);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(root)
                .setPositiveButton("确定", (d, w) -> {
                    int year = Integer.parseInt(etYear.getText().toString());
                    int month = selectedMonthInDialog[0];
                    if (year >= 2000 && year <= 2100 && month >= 1 && month <= 12) {
                        selectedYear = year;
                        selectedMonth = month;
                        selectedDay = 0;
                        navigateToMonth();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        dialog.show();
    }

    private void updateMonthButtonStyle(MaterialButton btn, boolean selected) {
        if (selected) {
            btn.setBackgroundColor(getResources().getColor(R.color.primary_teal, null));
            btn.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            btn.setBackgroundColor(getResources().getColor(R.color.chip_normal_bg, null));
            btn.setTextColor(getResources().getColor(R.color.text_primary, null));
        }
    }

    // ==================== ViewPager2 Adapter ====================

    private class BillPagerAdapter extends RecyclerView.Adapter<BillPagerAdapter.PageHolder> {

        class PageHolder extends RecyclerView.ViewHolder {
            PageHolder(View itemView) { super(itemView); }
        }

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.page_bill_list, parent, false);
            return new PageHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            RecyclerView rv = holder.itemView.findViewById(R.id.rv_page_bills);
            if (rv.getLayoutManager() == null) {
                rv.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            }
            loadPageData(rv, position);
        }

        @Override
        public int getItemCount() {
            return maxDay + 1;
        }
    }

    private void loadPageData(RecyclerView rv, int day) {
        List<Bill> bills;
        if (day == 0) {
            bills = dbHelper.getBillsByMonth(selectedYear, selectedMonth);
        } else {
            bills = dbHelper.getBillsByDay(selectedYear, selectedMonth, day);
        }

        if (!showSettled) {
            bills = filterUnsettled(bills);
        }

        TextView tvEmpty = ((View) rv.getParent()).findViewById(R.id.tv_page_empty);

        if (bills == null || bills.isEmpty()) {
            rv.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            BillAdapter adapter = new BillAdapter(bills);
            adapter.setOnBillClickListener(new BillAdapter.OnBillClickListener() {
                @Override
                public void onBillClick(Bill bill) {
                    showBillDetail(bill);
                }

                @Override
                public void onBillLongClick(View itemView, Bill bill) {
                    showPopupMenu(itemView, bill);
                }
            });
            rv.setAdapter(adapter);
        }
    }

    // ==================== 长按弹窗菜单（水平布局） ====================

    private void showPopupMenu(View anchor, Bill bill) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_bill_actions, null);
        PopupWindow popup = new PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(12);
        popup.setOutsideTouchable(true);

        MaterialButton btnEdit = popupView.findViewById(R.id.btn_popup_edit);
        MaterialButton btnDelete = popupView.findViewById(R.id.btn_popup_delete);
        View dividerUnsettle = popupView.findViewById(R.id.divider_unsettle);
        MaterialButton btnUnsettle = popupView.findViewById(R.id.btn_popup_unsettle);

        btnEdit.setOnClickListener(v -> {
            popup.dismiss();
            showEditDialog(bill);
        });
        btnDelete.setOnClickListener(v -> {
            popup.dismiss();
            showDeleteConfirm(bill);
        });

        if (bill.isSettled()) {
            dividerUnsettle.setVisibility(View.VISIBLE);
            btnUnsettle.setVisibility(View.VISIBLE);
            btnUnsettle.setOnClickListener(v2 -> {
                popup.dismiss();
                showUnsettleConfirm(bill);
            });
        } else {
            dividerUnsettle.setVisibility(View.GONE);
            btnUnsettle.setVisibility(View.GONE);
        }

        // 先测量尺寸
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int pw = popupView.getMeasuredWidth();
        int ph = popupView.getMeasuredHeight();

        // 获取 anchor 在屏幕上的位置
        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int anchorCenterX = loc[0] + anchor.getWidth() / 2;
        int anchorTop = loc[1];

        int x = anchorCenterX - pw / 2;
        int y = anchorTop - ph - 12;
        // 如果上方空间不够，显示在下方
        if (y < 80) {
            y = anchorTop + anchor.getHeight() + 12;
        }

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    // ==================== 编辑账单 ====================

    private void showEditDialog(Bill bill) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_bill, null);
        TextInputEditText etDate = dialogView.findViewById(R.id.et_edit_date);
        MaterialAutoCompleteTextView actvPerson = dialogView.findViewById(R.id.actv_edit_person);
        MaterialAutoCompleteTextView actvLocation = dialogView.findViewById(R.id.actv_edit_location);
        EditText etDescription = dialogView.findViewById(R.id.et_edit_description);
        EditText etAmount = dialogView.findViewById(R.id.et_edit_amount);

        // 填充现有数据
        etDate.setText(bill.getDate());
        actvPerson.setText(bill.getPersonName(), false);
        actvLocation.setText(bill.getLocation(), false);
        etDescription.setText(bill.getDescription());
        etAmount.setText(String.valueOf(bill.getAmount()));

        // 人员下拉
        List<String> people = dbHelper.getAllPeople();
        ArrayAdapter<String> personAdapter = new ArrayAdapter<>(this,
                R.layout.item_edit_dropdown, R.id.tv_dropdown_item, people);
        actvPerson.setAdapter(personAdapter);

        // 地点下拉
        List<String> locations = dbHelper.getAllLocations();
        ArrayAdapter<String> locationAdapter = new ArrayAdapter<>(this,
                R.layout.item_edit_dropdown, R.id.tv_dropdown_item, locations);
        actvLocation.setAdapter(locationAdapter);

        // 日期点击弹出日期选择器
        etDate.setOnClickListener(v -> {
            long billMillis = parseDateToMillis(bill.getDate());
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("选择日期")
                    .setSelection(billMillis)
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                cal.setTimeInMillis(selection);
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH));
                etDate.setText(date);
            });
            picker.show(getSupportFragmentManager(), "edit_date_picker");
        });

        new AlertDialog.Builder(this)
                .setTitle("编辑账单")
                .setView(dialogView)
                .setPositiveButton("保存", (d, which) -> {
                    String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
                    String person = actvPerson.getText().toString().trim();
                    String location = actvLocation.getText().toString().trim();
                    String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
                    String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

                    if (date.isEmpty() || person.isEmpty() || location.isEmpty()
                            || description.isEmpty() || amountStr.isEmpty()) {
                        Toast.makeText(this, "请填写所有字段", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try { amount = Double.parseDouble(amountStr); }
                    catch (NumberFormatException e) {
                        Toast.makeText(this, "金额格式不正确", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount <= 0) {
                        Toast.makeText(this, "金额必须大于0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    bill.setDate(date);
                    bill.setPersonName(person);
                    bill.setLocation(location);
                    bill.setDescription(description);
                    bill.setAmount(amount);
                    dbHelper.updateBill(bill);
                    refreshUI();
                    Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 删除账单 ====================

    private void showDeleteConfirm(Bill bill) {
        new AlertDialog.Builder(this)
                .setTitle("删除确认")
                .setMessage("确定要删除 " + bill.getPersonName() + " 在 "
                        + bill.getDate() + " 的这条记录吗？")
                .setPositiveButton("删除", (d, which) -> {
                    long billId = bill.getId();
                    dbHelper.softDeleteBill(billId);
                    refreshUI();

                    com.google.android.material.snackbar.Snackbar.make(
                            findViewById(R.id.main),
                            "已删除 " + bill.getPersonName() + " ¥" + String.format(Locale.getDefault(), "%.2f", bill.getAmount()),
                            com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                            .setAction("撤销", v -> {
                                dbHelper.restoreBill(billId);
                                refreshUI();
                            })
                            .show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private long parseDateToMillis(String yyyyMMdd) {
        try {
            String[] parts = yyyyMMdd.split("-");
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
            cal.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }

    // ==================== 账单详情 ====================

    private void showBillDetail(Bill bill) {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_bill_detail, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);

        TextView tvPerson = sheetView.findViewById(R.id.tv_detail_person);
        TextView tvAmount = sheetView.findViewById(R.id.tv_detail_amount);
        TextView tvDate = sheetView.findViewById(R.id.tv_detail_date);
        TextView tvLocation = sheetView.findViewById(R.id.tv_detail_location);
        TextView tvDesc = sheetView.findViewById(R.id.tv_detail_desc);
        TextView tvStatus = sheetView.findViewById(R.id.tv_detail_status);
        TextView tvCreated = sheetView.findViewById(R.id.tv_detail_created);

        tvPerson.setText(bill.getPersonName());
        tvAmount.setText(String.format(Locale.getDefault(), "¥%.2f", bill.getAmount()));
        tvDate.setText(formatDateChinese(bill.getDate()));
        tvLocation.setText(bill.getLocation());
        tvDesc.setText(bill.getDescription());

        if (bill.isSettled()) {
            tvStatus.setText("已结");
            tvStatus.setTextColor(getResources().getColor(R.color.settled_text, null));
        } else {
            tvStatus.setText("未结");
            tvStatus.setTextColor(getResources().getColor(R.color.amount_green, null));
        }

        String createdAt = bill.getCreatedAt();
        if (createdAt != null && !createdAt.isEmpty()) {
            tvCreated.setVisibility(View.VISIBLE);
            tvCreated.setText("记录于 " + createdAt);
        } else {
            tvCreated.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private String formatDateChinese(String yyyyMMdd) {
        try {
            String[] parts = yyyyMMdd.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            return String.format(Locale.getDefault(), "%d年%d月%d日", year, month, day);
        } catch (Exception e) {
            return yyyyMMdd;
        }
    }

    // ==================== 菜单 ====================

    private void startCoachMarkGuide() {
        List<CoachMarkStep> steps = Arrays.asList(
                new CoachMarkStep(R.id.btn_add,
                        R.string.coach_title_add, R.string.coach_desc_add, false),
                new CoachMarkStep(R.id.tv_year_month,
                        R.string.coach_title_month, R.string.coach_desc_month, false),
                new CoachMarkStep(R.id.hsv_days,
                        R.string.coach_title_day, R.string.coach_desc_day, false),
                new CoachMarkStep(0,
                        R.string.coach_title_edit, R.string.coach_desc_edit, false),
                new CoachMarkStep(R.id.btn_stats,
                        R.string.coach_title_stats, R.string.coach_desc_stats, true)
        );

        CoachMarkOverlay.startGuide(MainActivity.this, steps, () ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putBoolean(KEY_HAS_SEEN_MAIN_GUIDE, true)
                        .apply());
    }

    private void showMainMenu() {
        View sheetView = LayoutInflater.from(this).inflate(R.layout.popup_menu, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);

        sheetView.findViewById(R.id.item_trash).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, TrashActivity.class));
        });
        sheetView.findViewById(R.id.item_delete_person).setOnClickListener(v -> {
            dialog.dismiss();
            showDeletePersonBillsDialog();
        });
        sheetView.findViewById(R.id.item_clear_month).setOnClickListener(v -> {
            dialog.dismiss();
            showClearMonthConfirm();
        });

        sheetView.findViewById(R.id.item_lock_settings).setOnClickListener(v -> {
            dialog.dismiss();
            showLockSettingsDialog();
        });

        sheetView.findViewById(R.id.item_settle_bill).setOnClickListener(v -> {
            dialog.dismiss();
            showSettleDialog();
        });

        TextView tvShowSettled = sheetView.findViewById(R.id.item_show_settled);
        tvShowSettled.setText(showSettled ? R.string.menu_show_settled_on : R.string.menu_show_settled);
        tvShowSettled.setOnClickListener(v -> {
            showSettled = !showSettled;
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putBoolean(KEY_SHOW_SETTLED, showSettled).apply();
            dialog.dismiss();
            refreshUI();
        });

        sheetView.findViewById(R.id.item_help).setOnClickListener(v -> {
            dialog.dismiss();
            startCoachMarkGuide();
        });

        dialog.show();
    }

    // ==================== 删除某人全部账单 ====================

    private void showDeletePersonBillsDialog() {
        List<String> people = dbHelper.getAllPeople();
        java.util.LinkedHashSet<String> nameSet = new java.util.LinkedHashSet<>();
        nameSet.addAll(people);
        nameSet.addAll(dbHelper.getDistinctPeopleWithBills());
        List<String> allNames = new ArrayList<>(nameSet);

        if (allNames.isEmpty()) {
            Toast.makeText(this, "暂无人员", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_delete, null);
        LinearLayout layoutCheckboxList = dialogView.findViewById(R.id.layout_checkbox_list);
        boolean[] checked = new boolean[allNames.size()];
        List<CheckBox> checkBoxes = new ArrayList<>();

        for (int i = 0; i < allNames.size(); i++) {
            String name = allNames.get(i);
            int billCount = dbHelper.countBillsByPerson(name);
            CheckBox cb = (CheckBox) LayoutInflater.from(this)
                    .inflate(R.layout.item_location_checkbox, layoutCheckboxList, false);
            cb.setText(name + "（" + billCount + "条）");
            int index = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> checked[index] = isChecked);
            checkBoxes.add(cb);
            layoutCheckboxList.addView(cb);
        }

        new AlertDialog.Builder(this)
                .setTitle("勾选要删除的人员（可多选）")
                .setView(dialogView)
                .setPositiveButton("删除选中", (d, w) -> {
                    java.util.List<String> selected = new ArrayList<>();
                    int totalBills = 0;
                    for (int i = 0; i < allNames.size(); i++) {
                        if (checked[i]) {
                            selected.add(allNames.get(i));
                            totalBills += dbHelper.countBillsByPerson(allNames.get(i));
                        }
                    }
                    if (selected.isEmpty()) {
                        Toast.makeText(this, "请至少选择一人", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final int finalTotal = totalBills;
                    new AlertDialog.Builder(this)
                            .setTitle("确认批量删除")
                            .setMessage("确定要删除以下人员的全部账单吗？\n\n"
                                    + android.text.TextUtils.join("、", selected)
                                    + "\n\n共 " + finalTotal + " 条记录将移至回收站，可在回收站中恢复。")
                            .setPositiveButton("删除", (dd, ww) -> {
                                int total = 0;
                                for (String name : selected) {
                                    total += dbHelper.softDeleteBillsByPerson(name);
                                }
                                refreshUI();
                                Toast.makeText(this, "已删除" + total + "条记录（可恢复）",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 清空本月 ====================

    private void showClearMonthConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("确认")
                .setMessage("确定要清空 " + selectedYear + "年" + selectedMonth
                        + "月 的全部账单吗？\n\n数据将移至回收站，可在回收站中恢复。")
                .setPositiveButton("清空", (d, w) -> {
                    int count = dbHelper.softDeleteBillsByMonth(selectedYear, selectedMonth);
                    refreshUI();
                    Toast.makeText(this, "已清空" + count + "条记录（可恢复）",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 应用锁设置 ====================

    private AlertDialog settingsDialog;

    private void showLockSettingsDialog() {
        showLockSettingsDialogInternal();
    }

    private void showLockSettingsDialogInternal() {
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
        }

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_LOCK, MODE_PRIVATE);
        boolean hasPattern = !prefs.getString(Constants.KEY_LOCK_PATTERN, "").isEmpty();
        boolean hasPin = !prefs.getString(Constants.KEY_LOCK_PIN, "").isEmpty();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_lock_settings, null);
        TextView tvPatternStatus = dialogView.findViewById(R.id.tv_pattern_status);
        TextView tvPinStatus = dialogView.findViewById(R.id.tv_pin_status);
        MaterialButton btnPatternAction = dialogView.findViewById(R.id.btn_pattern_action);
        MaterialButton btnPatternDelete = dialogView.findViewById(R.id.btn_pattern_delete);
        MaterialButton btnPinAction = dialogView.findViewById(R.id.btn_pin_action);
        MaterialButton btnPinDelete = dialogView.findViewById(R.id.btn_pin_delete);

        updateLockRow(tvPatternStatus, btnPatternAction, btnPatternDelete, hasPattern, "图案锁");
        updateLockRow(tvPinStatus, btnPinAction, btnPinDelete, hasPin, "密码锁");

        btnPatternAction.setOnClickListener(v -> {
            settingsDialog.dismiss();
            showLockSetupDialog("pattern", this::showLockSettingsDialogInternal);
        });
        btnPinAction.setOnClickListener(v -> {
            settingsDialog.dismiss();
            showLockSetupDialog("pin", this::showLockSettingsDialogInternal);
        });
        btnPatternDelete.setOnClickListener(v -> {
            settingsDialog.dismiss();
            verifyAndDelete("pattern");
        });
        btnPinDelete.setOnClickListener(v -> {
            settingsDialog.dismiss();
            verifyAndDelete("pin");
        });

        settingsDialog = new AlertDialog.Builder(this)
                .setTitle("应用锁设置")
                .setView(dialogView)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void updateLockRow(TextView tvStatus, MaterialButton btnAction, MaterialButton btnDelete,
                               boolean isSet, String label) {
        if (isSet) {
            String text = label + "（已设置）";
            int green = getResources().getColor(R.color.amount_green, null);
            SpannableString sp = new SpannableString(text);
            int start = text.indexOf("（");
            sp.setSpan(new ForegroundColorSpan(green), start, text.length(), 0);
            tvStatus.setText(sp);
            btnAction.setText("修改");
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText(label);
            btnAction.setText("设置");
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void verifyAndDelete(String type) {
        verifyAndDelete(type, this::showLockSettingsDialogInternal);
    }

    private void verifyAndDelete(String type, Runnable onDone) {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_LOCK, MODE_PRIVATE);
        String key = "pattern".equals(type) ? Constants.KEY_LOCK_PATTERN : Constants.KEY_LOCK_PIN;
        String savedHash = prefs.getString(key, "");

        View setupView = LayoutInflater.from(this).inflate(R.layout.dialog_setup_lock, null);
        PatternLockView patternView = setupView.findViewById(R.id.pattern_setup);
        TextView tvSetupHint = setupView.findViewById(R.id.tv_setup_hint);
        LinearLayout pinSetupLayout = setupView.findViewById(R.id.layout_pin_setup);
        LinearLayout pinDotsLayout = setupView.findViewById(R.id.layout_setup_pin_dots);
        TextView tvPinMessage = setupView.findViewById(R.id.tv_setup_pin_message);

        MaterialButton btnPin0 = setupView.findViewById(R.id.btn_setup_pin_0);
        MaterialButton btnPin1 = setupView.findViewById(R.id.btn_setup_pin_1);
        MaterialButton btnPin2 = setupView.findViewById(R.id.btn_setup_pin_2);
        MaterialButton btnPin3 = setupView.findViewById(R.id.btn_setup_pin_3);
        MaterialButton btnPin4 = setupView.findViewById(R.id.btn_setup_pin_4);
        MaterialButton btnPin5 = setupView.findViewById(R.id.btn_setup_pin_5);
        MaterialButton btnPin6 = setupView.findViewById(R.id.btn_setup_pin_6);
        MaterialButton btnPin7 = setupView.findViewById(R.id.btn_setup_pin_7);
        MaterialButton btnPin8 = setupView.findViewById(R.id.btn_setup_pin_8);
        MaterialButton btnPin9 = setupView.findViewById(R.id.btn_setup_pin_9);
        MaterialButton btnPinDel = setupView.findViewById(R.id.btn_setup_pin_del);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("验证" + ("pattern".equals(type) ? "图案" : "密码"))
                .setView(setupView)
                .setNegativeButton("取消", null)
                .setOnDismissListener(d -> { if (onDone != null) onDone.run(); })
                .show();

        if ("pattern".equals(type)) {
            pinSetupLayout.setVisibility(View.GONE);
            patternView.setVisibility(View.VISIBLE);
            tvSetupHint.setText("请输入当前图案以确认删除");

            patternView.setOnPatternListener(pattern -> {
                if (CryptoUtils.sha256(pattern).equals(savedHash)) {
                    prefs.edit().remove(key).apply();
                    patternView.setStatus(PatternLockView.STATUS_CORRECT);
                    Toast.makeText(this, "图案锁已删除", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    patternView.setStatus(PatternLockView.STATUS_ERROR);
                    tvSetupHint.setText("图案不正确，请重试");
                    patternView.postDelayed(patternView::clearPattern, 800);
                }
            });
        } else {
            patternView.setVisibility(View.GONE);
            pinSetupLayout.setVisibility(View.VISIBLE);
            tvSetupHint.setText("请输入当前密码以确认删除");

            final StringBuilder pinBuilder = new StringBuilder();
            View.OnClickListener numListener = v -> {
                String tag = (String) v.getTag();
                if (pinBuilder.length() < 6) {
                    pinBuilder.append(tag);
                    updatePinSetupDots(pinDotsLayout, pinBuilder.length());
                    if (pinBuilder.length() == 6) {
                        if (CryptoUtils.sha256(pinBuilder.toString()).equals(savedHash)) {
                            prefs.edit().remove(key).apply();
                            tvPinMessage.setText("已删除");
                            tvPinMessage.setTextColor(0xFF4CAF50);
                            tvPinMessage.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "密码锁已删除", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            tvPinMessage.setText("密码不正确");
                            tvPinMessage.setTextColor(0xFFF44336);
                            tvPinMessage.setVisibility(View.VISIBLE);
                            pinBuilder.setLength(0);
                            updatePinSetupDots(pinDotsLayout, 0);
                        }
                    }
                }
            };

            btnPin0.setTag("0"); btnPin0.setOnClickListener(numListener);
            btnPin1.setTag("1"); btnPin1.setOnClickListener(numListener);
            btnPin2.setTag("2"); btnPin2.setOnClickListener(numListener);
            btnPin3.setTag("3"); btnPin3.setOnClickListener(numListener);
            btnPin4.setTag("4"); btnPin4.setOnClickListener(numListener);
            btnPin5.setTag("5"); btnPin5.setOnClickListener(numListener);
            btnPin6.setTag("6"); btnPin6.setOnClickListener(numListener);
            btnPin7.setTag("7"); btnPin7.setOnClickListener(numListener);
            btnPin8.setTag("8"); btnPin8.setOnClickListener(numListener);
            btnPin9.setTag("9"); btnPin9.setOnClickListener(numListener);
            btnPinDel.setOnClickListener(v -> {
                if (pinBuilder.length() > 0) {
                    pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                    updatePinSetupDots(pinDotsLayout, pinBuilder.length());
                }
            });
        }
    }

    private void showLockSetupDialog(String type, Runnable onDone) {
        View setupView = LayoutInflater.from(this).inflate(R.layout.dialog_setup_lock, null);
        PatternLockView patternView = setupView.findViewById(R.id.pattern_setup);
        TextView tvSetupHint = setupView.findViewById(R.id.tv_setup_hint);
        LinearLayout pinSetupLayout = setupView.findViewById(R.id.layout_pin_setup);
        MaterialButton btnPin0 = setupView.findViewById(R.id.btn_setup_pin_0);
        MaterialButton btnPin1 = setupView.findViewById(R.id.btn_setup_pin_1);
        MaterialButton btnPin2 = setupView.findViewById(R.id.btn_setup_pin_2);
        MaterialButton btnPin3 = setupView.findViewById(R.id.btn_setup_pin_3);
        MaterialButton btnPin4 = setupView.findViewById(R.id.btn_setup_pin_4);
        MaterialButton btnPin5 = setupView.findViewById(R.id.btn_setup_pin_5);
        MaterialButton btnPin6 = setupView.findViewById(R.id.btn_setup_pin_6);
        MaterialButton btnPin7 = setupView.findViewById(R.id.btn_setup_pin_7);
        MaterialButton btnPin8 = setupView.findViewById(R.id.btn_setup_pin_8);
        MaterialButton btnPin9 = setupView.findViewById(R.id.btn_setup_pin_9);
        MaterialButton btnPinDel = setupView.findViewById(R.id.btn_setup_pin_del);
        LinearLayout pinDotsLayout = setupView.findViewById(R.id.layout_setup_pin_dots);
        TextView tvPinMessage = setupView.findViewById(R.id.tv_setup_pin_message);

        final String[] firstPattern = {""};
        final String[] firstPin = {""};

        if ("pattern".equals(type)) {
            pinSetupLayout.setVisibility(View.GONE);
            patternView.setVisibility(View.VISIBLE);
            tvSetupHint.setText("请绘制解锁图案（至少4个点）");

            patternView.setOnPatternListener(pattern -> {
                if (firstPattern[0].isEmpty()) {
                    firstPattern[0] = pattern;
                    tvSetupHint.setText("请再次绘制以确认");
                    patternView.clearPattern();
                } else {
                    if (pattern.equals(firstPattern[0])) {
                        String hash = CryptoUtils.sha256(pattern);
                        getSharedPreferences(Constants.PREFS_LOCK, MODE_PRIVATE).edit()
                                .putString(Constants.KEY_LOCK_PATTERN, hash).apply();
                        patternView.setStatus(PatternLockView.STATUS_CORRECT);
                        Toast.makeText(this, "图案锁设置成功", Toast.LENGTH_SHORT).show();
                    } else {
                        patternView.setStatus(PatternLockView.STATUS_ERROR);
                        tvSetupHint.setText("两次图案不一致，请重新绘制");
                        firstPattern[0] = "";
                        patternView.postDelayed(patternView::clearPattern, 800);
                    }
                }
            });
        } else {
            patternView.setVisibility(View.GONE);
            pinSetupLayout.setVisibility(View.VISIBLE);
            tvSetupHint.setText("请设置密码（6位数字）");

            final StringBuilder pinBuilder = new StringBuilder();
            View.OnClickListener numListener = v -> {
                String tag = (String) v.getTag();
                if (pinBuilder.length() < 6) {
                    pinBuilder.append(tag);
                    updatePinSetupDots(pinDotsLayout, pinBuilder.length());
                    if (pinBuilder.length() == 6) {
                        if (firstPin[0].isEmpty()) {
                            firstPin[0] = pinBuilder.toString();
                            tvSetupHint.setText("请再次输入以确认");
                            tvPinMessage.setVisibility(View.GONE);
                            pinBuilder.setLength(0);
                            updatePinSetupDots(pinDotsLayout, 0);
                        } else {
                            if (pinBuilder.toString().equals(firstPin[0])) {
                                String hash = CryptoUtils.sha256(pinBuilder.toString());
                                getSharedPreferences(Constants.PREFS_LOCK, MODE_PRIVATE).edit()
                                        .putString(Constants.KEY_LOCK_PIN, hash).apply();
                                tvPinMessage.setText("密码设置成功");
                                tvPinMessage.setTextColor(0xFF4CAF50);
                                tvPinMessage.setVisibility(View.VISIBLE);
                                Toast.makeText(this, "密码锁设置成功", Toast.LENGTH_SHORT).show();
                            } else {
                                tvPinMessage.setText("两次密码不一致");
                                tvPinMessage.setTextColor(0xFFF44336);
                                tvPinMessage.setVisibility(View.VISIBLE);
                                firstPin[0] = "";
                                pinBuilder.setLength(0);
                                updatePinSetupDots(pinDotsLayout, 0);
                                tvSetupHint.setText("请重新设置密码（6位数字）");
                            }
                        }
                    }
                }
            };

            btnPin0.setTag("0"); btnPin0.setOnClickListener(numListener);
            btnPin1.setTag("1"); btnPin1.setOnClickListener(numListener);
            btnPin2.setTag("2"); btnPin2.setOnClickListener(numListener);
            btnPin3.setTag("3"); btnPin3.setOnClickListener(numListener);
            btnPin4.setTag("4"); btnPin4.setOnClickListener(numListener);
            btnPin5.setTag("5"); btnPin5.setOnClickListener(numListener);
            btnPin6.setTag("6"); btnPin6.setOnClickListener(numListener);
            btnPin7.setTag("7"); btnPin7.setOnClickListener(numListener);
            btnPin8.setTag("8"); btnPin8.setOnClickListener(numListener);
            btnPin9.setTag("9"); btnPin9.setOnClickListener(numListener);

            btnPinDel.setOnClickListener(v -> {
                if (pinBuilder.length() > 0) {
                    pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                    updatePinSetupDots(pinDotsLayout, pinBuilder.length());
                }
            });
        }

        new AlertDialog.Builder(this)
                .setTitle("设置" + ("pattern".equals(type) ? "图案" : "密码") + "锁")
                .setView(setupView)
                .setNegativeButton("取消", null)
                .setOnDismissListener(d -> { if (onDone != null) onDone.run(); })
                .show();
    }

    private void updatePinSetupDots(LinearLayout container, int count) {
        container.removeAllViews();
        for (int i = 0; i < 6; i++) {
            View dot = new View(this);
            int size = dpToPx(12);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(dpToPx(6), 0, dpToPx(6), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i < count
                    ? R.drawable.dot_selected : R.drawable.dot_unselected);
            container.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return DisplayUtils.dpToPx(dp);
    }

    // ==================== 已结账单 ====================

    private List<Bill> filterUnsettled(List<Bill> bills) {
        if (bills == null) return new ArrayList<>();
        List<Bill> result = new ArrayList<>();
        for (Bill b : bills) {
            if (!b.isSettled()) result.add(b);
        }
        return result;
    }

    private void showSettleDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settle_bill, null);
        MaterialCardView cardPerson = dialogView.findViewById(R.id.card_settle_person);
        TextView tvSettlePerson = dialogView.findViewById(R.id.tv_settle_person);
        MaterialCardView cardStart = dialogView.findViewById(R.id.card_settle_start);
        MaterialCardView cardEnd = dialogView.findViewById(R.id.card_settle_end);
        TextView tvStart = dialogView.findViewById(R.id.tv_settle_start);
        TextView tvEnd = dialogView.findViewById(R.id.tv_settle_end);
        TextView tvPreview = dialogView.findViewById(R.id.tv_settle_preview);

        Calendar today = Calendar.getInstance();
        String defaultStart, defaultEnd;
        today.set(Calendar.DAY_OF_MONTH, 1);
        defaultStart = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, 1);
        defaultEnd = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1,
                today.getActualMaximum(Calendar.DAY_OF_MONTH));
        tvStart.setText(defaultStart);
        tvEnd.setText(defaultEnd);

        final String[] startDate = {defaultStart};
        final String[] endDate = {defaultEnd};
        final String[] selectedPerson = {""};

        List<String> people = dbHelper.getAllPeople();
        if (people.isEmpty()) {
            Toast.makeText(this, R.string.empty_list, Toast.LENGTH_SHORT).show();
            return;
        }
        final String[] peopleArr = people.toArray(new String[0]);

        cardPerson.setOnClickListener(vp -> {
            ArrayAdapter<String> personDialogAdapter = new ArrayAdapter<>(this,
                    R.layout.item_edit_dropdown, R.id.tv_dropdown_item, peopleArr);
            new AlertDialog.Builder(this)
                    .setTitle("选择人员（共" + peopleArr.length + "人）")
                    .setAdapter(personDialogAdapter, (d2, which2) -> {
                        selectedPerson[0] = peopleArr[which2];
                        tvSettlePerson.setText(selectedPerson[0]);
                        updateSettlePreview(dbHelper, selectedPerson[0], startDate[0], endDate[0], tvPreview);
                    })
                    .show();
        });

        cardStart.setOnClickListener(v1 -> {
            long millis = parseDateToMillis(startDate[0]);
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("选择开始日期").setSelection(millis).build();
            picker.addOnPositiveButtonClickListener(sel -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                cal.setTimeInMillis(sel);
                startDate[0] = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                tvStart.setText(startDate[0]);
                updateSettlePreview(dbHelper, selectedPerson[0], startDate[0], endDate[0], tvPreview);
            });
            picker.show(getSupportFragmentManager(), "settle_start");
        });

        cardEnd.setOnClickListener(v1 -> {
            long millis = parseDateToMillis(endDate[0]);
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("选择结束日期").setSelection(millis).build();
            picker.addOnPositiveButtonClickListener(sel -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                cal.setTimeInMillis(sel);
                endDate[0] = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                tvEnd.setText(endDate[0]);
                updateSettlePreview(dbHelper, selectedPerson[0], startDate[0], endDate[0], tvPreview);
            });
            picker.show(getSupportFragmentManager(), "settle_end");
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.settle_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.settle_confirm, (d, which) -> {
                    if (selectedPerson[0].isEmpty()) {
                        Toast.makeText(this, R.string.settle_select_person, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int count = dbHelper.settleBillsByPersonAndDateRange(
                            selectedPerson[0], startDate[0], endDate[0]);
                    if (count > 0) {
                        refreshUI();
                        Toast.makeText(this,
                                String.format(Locale.getDefault(),
                                        getString(R.string.settle_success), count),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.settle_no_bills, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateSettlePreview(BillDatabaseHelper helper, String person,
                                     String startDate, String endDate, TextView tvPreview) {
        if (person == null || person.isEmpty()) {
            tvPreview.setVisibility(View.INVISIBLE);
            return;
        }
        int count = helper.countBillsByPersonAndDateRange(person, startDate, endDate);
        double sum = helper.sumBillsByPersonAndDateRange(person, startDate, endDate);
        tvPreview.setText(String.format(Locale.getDefault(),
                getString(R.string.settle_preview_format), count,
                String.format(Locale.getDefault(), "%.2f", sum)));
        tvPreview.setVisibility(View.VISIBLE);
    }

    private void showUnsettleConfirm(Bill bill) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.unsettle_confirm_title)
                .setMessage(String.format(Locale.getDefault(),
                        getString(R.string.unsettle_confirm_msg), bill.getPersonName()))
                .setPositiveButton("确定", (d, which) -> {
                    dbHelper.unsettleBill(bill.getId());
                    refreshUI();
                    Toast.makeText(this, R.string.unsettle_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
