package com.example.mybill;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mybill.db.BillDatabaseHelper;
import com.example.mybill.model.Bill;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;

import com.example.mybill.util.CsvUtils;
import com.example.mybill.util.DisplayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class StatisticsActivity extends AppCompatActivity {

    private TextView tvStartDate, tvEndDate, tvRangeDays;
    private ChipGroup chipGroupPeople;
    private ViewPager2 viewPagerStats;
    private LinearLayout layoutDots;
    private TextView dotPage0, dotPage1;
    private BillDatabaseHelper dbHelper;
    private StatsPagerAdapter statsPagerAdapter;

    // 缓存最近一次统计结果，供 ViewPager2 两个页面使用
    private Map<String, List<Bill>> lastPersonBills;
    private List<Bill> lastBillsList;

    private String startDate, endDate;
    private final List<String> allPeople = new ArrayList<>();
    private final List<Chip> peopleChips = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportFile);

    @Override
    protected void attachBaseContext(Context newBase) {
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        config.setLocale(Locale.SIMPLIFIED_CHINESE);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_stats), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new BillDatabaseHelper(this);

        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        tvRangeDays = findViewById(R.id.tv_range_days);
        chipGroupPeople = findViewById(R.id.chip_group_people);
        viewPagerStats = findViewById(R.id.view_pager_stats);
        layoutDots = findViewById(R.id.layout_dots);
        dotPage0 = findViewById(R.id.dot_page0);
        dotPage1 = findViewById(R.id.dot_page1);
        MaterialButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnStats = findViewById(R.id.btn_stats);
        MaterialButton btnExport = findViewById(R.id.btn_export);
        MaterialButton btnImport = findViewById(R.id.btn_import);
        MaterialCardView cardStartDate = findViewById(R.id.card_start_date);
        MaterialCardView cardEndDate = findViewById(R.id.card_end_date);

        Calendar today = Calendar.getInstance();
        endDate = sdf.format(today.getTime());
        today.set(Calendar.DAY_OF_MONTH, 1);
        startDate = sdf.format(today.getTime());
        tvStartDate.setText(startDate);
        tvEndDate.setText(endDate);
        updateRangeDays();

        cardStartDate.setOnClickListener(v -> showDatePicker(true));
        cardEndDate.setOnClickListener(v -> showDatePicker(false));

        refreshPeopleChips();

        btnStats.setOnClickListener(v -> calculateStats());
        btnExport.setOnClickListener(v -> backupData());
        btnImport.setOnClickListener(v -> importLauncher.launch(new String[]{"text/*", "*/*"}));
        btnBack.setOnClickListener(v -> finish());

        statsPagerAdapter = new StatsPagerAdapter();
        viewPagerStats.setAdapter(statsPagerAdapter);
        viewPagerStats.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
            }
        });

        handleImportIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleImportIntent(intent);
    }

    private void handleImportIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri uri = intent.getData();
            handleImportFile(uri);
        }
    }

    // ==================== 一键备份 ====================

    private void backupData() {
        List<Bill> bills = dbHelper.getAllBills();
        List<String> people = dbHelper.getAllPeople();
        List<String> locations = dbHelper.getAllLocations();

        if (bills.isEmpty() && people.isEmpty() && locations.isEmpty()) {
            Toast.makeText(this, "没有数据可备份", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("===人员===\n");
        for (String p : people) sb.append(CsvUtils.escape(p)).append("\n");

        sb.append("===地点===\n");
        for (String l : locations) sb.append(CsvUtils.escape(l)).append("\n");

        sb.append("===账单===\n");
        sb.append("\"日期\",\"人员\",\"地点\",\"事项\",\"金额\"\n");
        for (Bill b : bills) {
            sb.append(CsvUtils.escape(b.getDate())).append(",")
                    .append(CsvUtils.escape(b.getPersonName())).append(",")
                    .append(CsvUtils.escape(b.getLocation())).append(",")
                    .append(CsvUtils.escape(b.getDescription())).append(",")
                    .append(CsvUtils.escape(String.format(Locale.getDefault(), "%.2f", b.getAmount())))
                    .append("\n");
        }

        String data = sb.toString();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        String fileName = "账单备份_" + today + ".csv";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // API 29+: 直接用 MediaStore 写入 Downloads
            try {
                android.content.ContentResolver resolver = getContentResolver();

                // 删除今天已有的同名备份，防止重复生成 (1)(2) 文件
                resolver.delete(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        android.provider.MediaStore.Downloads.DISPLAY_NAME + "=?",
                        new String[]{fileName});

                android.content.ContentValues values = new android.content.ContentValues();
                values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH,
                        android.os.Environment.DIRECTORY_DOWNLOADS);

                Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    java.io.OutputStream os = resolver.openOutputStream(uri);
                    if (os != null) {
                        os.write(data.getBytes(StandardCharsets.UTF_8));
                        os.close();
                        showBackupSuccessDialog(fileName);
                        return;
                    }
                }
            } catch (Exception e) {
                // 回退到分享方式
            }
        }

        // API < 29 或 MediaStore 失败：回退到分享
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, data);
        startActivity(Intent.createChooser(share, "备份数据"));
    }

    private void showBackupSuccessDialog(String fileName) {
        new AlertDialog.Builder(this)
                .setTitle("备份完成")
                .setMessage("已保存到下载文件夹：" + fileName
                        + "\n\n建议发送到微信或邮箱保存，防止手机丢失")
                .setPositiveButton("分享文件", (d, w) -> {
                    try {
                        // 从 Downloads 读取刚保存的文件并分享
                        android.content.ContentResolver resolver = getContentResolver();
                        android.database.Cursor cursor = resolver.query(
                                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                new String[]{android.provider.MediaStore.Downloads._ID,
                                        android.provider.MediaStore.Downloads.DISPLAY_NAME},
                                android.provider.MediaStore.Downloads.DISPLAY_NAME + "=?",
                                new String[]{fileName}, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            long id = cursor.getLong(0);
                            Uri fileUri = android.content.ContentUris.withAppendedId(
                                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                            Intent share = new Intent(Intent.ACTION_SEND);
                            share.setType("text/csv");
                            share.putExtra(Intent.EXTRA_STREAM, fileUri);
                            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(share, "分享备份文件"));
                        }
                        if (cursor != null) cursor.close();
                    } catch (Exception ignored) {
                    }
                })
                .setNegativeButton("知道了", null)
                .show();
    }

    // ==================== 导入 ====================

    private void handleImportFile(Uri uri) {
        if (uri == null) return;
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) {
                Toast.makeText(this, "无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            List<String> newPeople = new ArrayList<>();
            List<String> newLocations = new ArrayList<>();
            List<Bill> newBills = new ArrayList<>();
            int skippedInvalid = 0;
            boolean firstLine = true;

            String line;
            String section = "";
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 去除 BOM 头 (U+FEFF)
                if (firstLine && !line.isEmpty()) {
                    firstLine = false;
                    if (line.codePointAt(0) == 0xFEFF) {
                        line = line.substring(1);
                    }
                }

                if (line.isEmpty()) continue;
                if (line.startsWith("===人员===")) { section = "people"; continue; }
                if (line.startsWith("===地点===")) { section = "locations"; continue; }
                if (line.startsWith("===账单===")) { section = "bills"; headerSkipped = false; continue; }

                switch (section) {
                    case "people":
                        newPeople.add(CsvUtils.unescape(line));
                        break;
                    case "locations":
                        newLocations.add(CsvUtils.unescape(line));
                        break;
                    case "bills":
                        if (!headerSkipped && line.startsWith("\"")) { headerSkipped = true; continue; }
                        headerSkipped = true;
                        String[] parts = CsvUtils.parseLine(line);
                        if (parts.length >= 5) {
                            double amount;
                            try { amount = Double.parseDouble(parts[4].trim()); }
                            catch (NumberFormatException e) { skippedInvalid++; continue; }
                            newBills.add(new Bill(parts[0].trim(), parts[1].trim(),
                                    parts[2].trim(), parts[3].trim(), amount));
                        } else {
                            skippedInvalid++;
                        }
                        break;
                }
            }
            reader.close();

            if (newBills.isEmpty()) {
                Toast.makeText(this, "文件中没有找到账单数据", Toast.LENGTH_SHORT).show();
                return;
            }

            final int finalSkippedInvalid = skippedInvalid;
            new AlertDialog.Builder(this)
                    .setTitle("确认导入")
                    .setMessage(String.format(Locale.getDefault(),
                            "将导入 %d 条账单、%d 个人员、%d 个地点。\n已存在的记录将自动跳过。",
                            newBills.size(), newPeople.size(), newLocations.size()))
                    .setPositiveButton("导入", (d, w) -> {
                        int peopleAdded = 0, locAdded = 0, billsAdded = 0, skipped = 0;
                        for (String p : newPeople) {
                            if (dbHelper.insertPerson(p) != -1) peopleAdded++;
                        }
                        for (String l : newLocations) {
                            if (dbHelper.insertLocation(l) != -1) locAdded++;
                        }
                        for (Bill b : newBills) {
                            if (!dbHelper.billExists(b.getDate(), b.getPersonName(),
                                    b.getLocation(), b.getDescription(), b.getAmount())) {
                                dbHelper.insertBill(b);
                                billsAdded++;
                            } else {
                                skipped++;
                            }
                        }

                        StringBuilder msg = new StringBuilder();
                        msg.append(String.format(Locale.getDefault(),
                                "导入完成\n人员: %d 新\n地点: %d 新\n账单: %d 条新增, %d 条跳过",
                                peopleAdded, locAdded, billsAdded, skipped));
                        if (finalSkippedInvalid > 0) {
                            msg.append(String.format(Locale.getDefault(),
                                    "\n跳过 %d 条格式错误的数据", finalSkippedInvalid));
                        }
                        new AlertDialog.Builder(this)
                                .setTitle("导入结果")
                                .setMessage(msg.toString())
                                .setPositiveButton("确定", null)
                                .show();

                        refreshPeopleChips();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ==================== 以下为原有统计逻辑 ====================

    private void showDatePicker(boolean isStart) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStart ? "选择开始日期" : "选择结束日期")
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
            cal.setTimeInMillis(selection);
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
            if (isStart) {
                startDate = date;
                tvStartDate.setText(date);
            } else {
                endDate = date;
                tvEndDate.setText(date);
            }
            updateRangeDays();
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void updateRangeDays() {
        try {
            java.util.Date start = sdf.parse(startDate);
            java.util.Date end = sdf.parse(endDate);
            if (start != null && end != null) {
                long diff = end.getTime() - start.getTime();
                long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
                tvRangeDays.setText(String.format(Locale.getDefault(), "共%d天", days));
            }
        } catch (ParseException e) {
            tvRangeDays.setText("");
        }
    }

    private boolean chipUpdating = false;

    private void refreshPeopleChips() {
        chipGroupPeople.removeAllViews();
        peopleChips.clear();
        allPeople.clear();

        Chip chipAll = new Chip(this);
        chipAll.setText(getString(R.string.all));
        chipAll.setCheckable(true);
        chipAll.setChecked(true);
        chipGroupPeople.addView(chipAll);
        peopleChips.add(chipAll);

        // 合并人员表和账单表中出现过的人员，去重
        java.util.LinkedHashSet<String> nameSet = new java.util.LinkedHashSet<>();
        nameSet.addAll(dbHelper.getAllPeople());
        nameSet.addAll(dbHelper.getDistinctPeopleWithBills());
        List<String> people = new ArrayList<>(nameSet);

        for (String name : people) {
            Chip chip = new Chip(this);
            chip.setText(name);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                if (chipUpdating) return;
                // 选中具体人员时取消「全部」
                chipUpdating = true;
                if (chip.isChecked()) {
                    peopleChips.get(0).setChecked(false);
                }
                chipUpdating = false;
            });
            chipGroupPeople.addView(chip);
            peopleChips.add(chip);
            allPeople.add(name);
        }

        chipAll.setOnClickListener(v -> {
            if (chipUpdating) return;
            // 「全部」被点击时，取消所有具体人员
            chipUpdating = true;
            if (chipAll.isChecked()) {
                for (int i = 1; i < peopleChips.size(); i++) {
                    peopleChips.get(i).setChecked(false);
                }
            } else {
                chipAll.setChecked(true); // 「全部」不可取消
            }
            chipUpdating = false;
        });
    }

    private void calculateStats() {
        List<String> selectedPeople = new ArrayList<>();
        boolean selectAll = peopleChips.get(0).isChecked();
        if (!selectAll) {
            for (int i = 1; i < peopleChips.size(); i++) {
                if (peopleChips.get(i).isChecked()) {
                    selectedPeople.add(allPeople.get(i - 1));
                }
            }
            if (selectedPeople.isEmpty()) selectedPeople = new ArrayList<>();
        } else {
            selectedPeople = null;
        }

        List<Bill> bills = dbHelper.getBillsByDateRange(startDate, endDate, null);
        if (selectedPeople != null) {
            List<Bill> filtered = new ArrayList<>();
            for (Bill b : bills) {
                if (selectedPeople.contains(b.getPersonName())) {
                    filtered.add(b);
                }
            }
            bills = filtered;
        }

        if (bills.isEmpty()) {
            lastPersonBills = null;
            lastBillsList = null;
            layoutDots.setVisibility(View.GONE);
        } else {
            Map<String, List<Bill>> personBills = new LinkedHashMap<>();
            for (Bill bill : bills) {
                String name = bill.getPersonName();
                if (!personBills.containsKey(name)) {
                    personBills.put(name, new ArrayList<>());
                }
                personBills.get(name).add(bill);
            }
            lastPersonBills = personBills;
            lastBillsList = bills;
            layoutDots.setVisibility(View.VISIBLE);
        }

        statsPagerAdapter.notifyDataSetChanged();
        viewPagerStats.setCurrentItem(0, false);
        updateDots(0);
    }

    private void showPieChart(PieChart chartPie, Map<String, List<Bill>> personBills) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Bill>> e : personBills.entrySet()) {
            double total = 0;
            for (Bill b : e.getValue()) total += b.getAmount();
            entries.add(new PieEntry((float) total, e.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "¥%.0f", value);
            }
        });

        PieData data = new PieData(dataSet);
        chartPie.setData(data);
        chartPie.setUsePercentValues(false);
        chartPie.getDescription().setEnabled(false);
        chartPie.setDrawHoleEnabled(true);
        chartPie.setHoleRadius(40f);
        chartPie.setTransparentCircleRadius(44f);
        chartPie.setEntryLabelTextSize(12f);
        chartPie.getLegend().setTextSize(12f);
        chartPie.setNoDataText("暂无数据");
        chartPie.animateY(1500);
    }

    private void showBarChart(BarChart chartBar, List<Bill> bills) {
        Map<String, Double> monthlyTotals = new LinkedHashMap<>();
        for (Bill b : bills) {
            String monthKey = b.getDate().length() >= 7 ? b.getDate().substring(0, 7) : b.getDate();
            Double cur = monthlyTotals.get(monthKey);
            monthlyTotals.put(monthKey, (cur != null ? cur : 0) + b.getAmount());
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> e : monthlyTotals.entrySet()) {
            entries.add(new BarEntry(i, e.getValue().floatValue()));
            labels.add(e.getKey());
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "月总额");
        dataSet.setColor(0xFF00897B);
        dataSet.setValueTextSize(11f);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "¥%.0f", value);
            }
        });

        BarData data = new BarData(dataSet);
        chartBar.setData(data);
        chartBar.getDescription().setEnabled(false);
        chartBar.setNoDataText("暂无数据");
        chartBar.setFitBars(true);

        XAxis xAxis = chartBar.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45f);
        chartBar.getAxisRight().setEnabled(false);
        chartBar.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "¥%.0f", value);
            }
        });
        chartBar.animateY(1200);
    }

    private void updateDots(int position) {
        if (position == 0) {
            dotPage0.setTextColor(getResources().getColor(R.color.primary_teal, null));
            dotPage1.setTextColor(getResources().getColor(R.color.chip_normal_bg, null));
        } else {
            dotPage0.setTextColor(getResources().getColor(R.color.chip_normal_bg, null));
            dotPage1.setTextColor(getResources().getColor(R.color.primary_teal, null));
        }
    }

    // ==================== ViewPager2 适配器 ====================

    private class StatsPagerAdapter extends RecyclerView.Adapter<StatsPagerAdapter.PageHolder> {

        class PageHolder extends RecyclerView.ViewHolder {
            PageHolder(View itemView) { super(itemView); }
        }

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                ScrollView sv = new ScrollView(parent.getContext());
                sv.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                LinearLayout root = new LinearLayout(parent.getContext());
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(16, 12, 16, 32);
                sv.addView(root);
                return new PageHolder(sv);
            } else {
                ScrollView sv = new ScrollView(parent.getContext());
                sv.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                View chartsView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.page_stats_charts, parent, false);
                sv.addView(chartsView);
                return new PageHolder(sv);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            if (position == 0) {
                LinearLayout root = (LinearLayout) ((ScrollView) holder.itemView).getChildAt(0);
                root.removeAllViews();

                if (lastPersonBills == null || lastPersonBills.isEmpty()) {
                    TextView tv = new TextView(StatisticsActivity.this);
                    tv.setText("暂无账单记录");
                    tv.setTextSize(15);
                    tv.setTextColor(getResources().getColor(R.color.text_secondary, null));
                    tv.setGravity(android.view.Gravity.CENTER);
                    tv.setPadding(0, 60, 0, 60);
                    root.addView(tv);
                    return;
                }

                double grandAmount = 0;
                for (Map.Entry<String, List<Bill>> entry : lastPersonBills.entrySet()) {
                    String name = entry.getKey();
                    List<Bill> pBills = entry.getValue();

                    double totalAmount = 0;
                    List<String> dates = new ArrayList<>();
                    for (Bill b : pBills) {
                        totalAmount += b.getAmount();
                        if (!dates.contains(b.getDate())) dates.add(b.getDate());
                    }
                    grandAmount += totalAmount;

                    View card = LayoutInflater.from(StatisticsActivity.this)
                            .inflate(R.layout.item_statistic_person, root, false);
                    TextView tvName = card.findViewById(R.id.tv_person_name);
                    TextView tvTotal = card.findViewById(R.id.tv_person_total);
                    LinearLayout layoutHeader = card.findViewById(R.id.layout_person_header);
                    LinearLayout layoutDetails = card.findViewById(R.id.layout_details);

                    tvName.setText(name);
                    tvTotal.setText(String.format(Locale.getDefault(), "¥%.2f / %d天", totalAmount, dates.size()));

                    MaterialButton btnSharePerson = card.findViewById(R.id.btn_share_person);
                    btnSharePerson.setOnClickListener(v -> showShareOptions(name, pBills));

                    layoutDetails.setVisibility(View.GONE);
                    layoutHeader.setOnClickListener(v -> {
                        if (layoutDetails.getVisibility() == View.VISIBLE) {
                            collapse(layoutDetails);
                            btnSharePerson.animate().rotation(0f).setDuration(200).start();
                        } else {
                            expand(layoutDetails);
                            btnSharePerson.animate().rotation(180f).setDuration(200).start();
                        }
                    });

                    for (Bill b : pBills) {
                        View detailRow = LayoutInflater.from(StatisticsActivity.this)
                                .inflate(R.layout.item_statistic_detail, layoutDetails, false);
                        TextView tvDate = detailRow.findViewById(R.id.tv_detail_date);
                        TextView tvLoc = detailRow.findViewById(R.id.tv_detail_location);
                        TextView tvAmt = detailRow.findViewById(R.id.tv_detail_amount);

                        String dateStr = b.getDate();
                        if (dateStr.length() >= 10) dateStr = dateStr.substring(5);
                        tvDate.setText(dateStr);
                        tvLoc.setText(b.getLocation());
                        tvAmt.setText(String.format(Locale.getDefault(), "¥%.2f", b.getAmount()));
                        layoutDetails.addView(detailRow);
                    }

                    root.addView(card);
                }

                TextView tvGrandTotal = new TextView(StatisticsActivity.this);
                tvGrandTotal.setText(String.format(Locale.getDefault(),
                        "合计: %d人, 总额 ¥%.2f", lastPersonBills.size(), grandAmount));
                tvGrandTotal.setTextSize(16);
                tvGrandTotal.setTextColor(getResources().getColor(R.color.primary_teal, null));
                tvGrandTotal.setGravity(android.view.Gravity.CENTER);
                tvGrandTotal.setPadding(12, 12, 12, 12);
                tvGrandTotal.setBackgroundColor(0xFFFFFFFF);
                root.addView(tvGrandTotal, 0);

            } else {
                View chartsView = ((ScrollView) holder.itemView).getChildAt(0);
                PieChart chartPie = chartsView.findViewById(R.id.chart_pie);
                BarChart chartBar = chartsView.findViewById(R.id.chart_bar);

                if (lastPersonBills != null && !lastPersonBills.isEmpty()) {
                    showPieChart(chartPie, lastPersonBills);
                    showBarChart(chartBar, lastBillsList);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    // ==================== 分享某人账单 ====================

    private void showShareOptions(String personName, List<Bill> bills) {
        new AlertDialog.Builder(this)
                .setTitle("分享 " + personName + " 的账单")
                .setItems(new String[]{"分享为图片", "分享为表格文件"}, (dialog, which) -> {
                    if (which == 0) {
                        shareAsImage(personName, bills);
                    } else {
                        shareAsCsv(personName, bills);
                    }
                })
                .show();
    }

    private void shareAsImage(String personName, List<Bill> bills) {
        try {
            View cardView = LayoutInflater.from(this).inflate(R.layout.card_share_bill, null);

            // 标题
            TextView tvTitle = cardView.findViewById(R.id.tv_share_title);
            tvTitle.setText(personName + " 账单明细");

            // 日期范围
            TextView tvDateRange = cardView.findViewById(R.id.tv_share_date_range);
            String rangeText = startDate + " 至 " + endDate;
            if (startDate.equals(endDate)) {
                rangeText = startDate;
            }
            tvDateRange.setText(rangeText);

            // 汇总
            double total = 0;
            for (Bill b : bills) total += b.getAmount();
            TextView tvCount = cardView.findViewById(R.id.tv_share_count);
            tvCount.setText(String.format(Locale.getDefault(), "共 %d 笔", bills.size()));
            TextView tvTotal = cardView.findViewById(R.id.tv_share_total);
            tvTotal.setText(String.format(Locale.getDefault(), "¥%.2f", total));

            // 明细
            LinearLayout layoutDetails = cardView.findViewById(R.id.layout_share_details);
            for (Bill b : bills) {
                View row = LayoutInflater.from(this)
                        .inflate(R.layout.item_statistic_detail, layoutDetails, false);
                TextView tvDate = row.findViewById(R.id.tv_detail_date);
                TextView tvLoc = row.findViewById(R.id.tv_detail_location);
                TextView tvAmt = row.findViewById(R.id.tv_detail_amount);

                String dateStr = b.getDate();
                if (dateStr.length() >= 10) {
                    dateStr = dateStr.substring(5); // 显示 MM-dd
                }
                tvDate.setText(dateStr);
                tvLoc.setText(b.getLocation());
                tvAmt.setText(String.format(Locale.getDefault(), "¥%.2f", b.getAmount()));
                layoutDetails.addView(row);
            }

            // 测量并绘制
            int widthSpec = View.MeasureSpec.makeMeasureSpec(
                    getResources().getDisplayMetrics().widthPixels - DisplayUtils.dpToPx(32),
                    View.MeasureSpec.AT_MOST);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            cardView.measure(widthSpec, heightSpec);
            cardView.layout(0, 0, cardView.getMeasuredWidth(), cardView.getMeasuredHeight());

            Bitmap bitmap = Bitmap.createBitmap(cardView.getMeasuredWidth(),
                    cardView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(0xFFFFFFFF);
            cardView.draw(canvas);

            // 保存到缓存
            File dir = new File(getCacheDir(), "shares");
            if (!dir.exists()) dir.mkdirs();
            String safeName = personName.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fff]", "");
            File file = new File(dir, "bill_" + safeName + "_" + startDate + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            // 分享
            Uri shareUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, shareUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "分享账单图片"));

        } catch (Exception e) {
            Toast.makeText(this, "生成图片失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareAsCsv(String personName, List<Bill> bills) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\"日期\",\"人员\",\"地点\",\"事项\",\"金额\"\n");
            for (Bill b : bills) {
                sb.append(CsvUtils.escape(b.getDate())).append(",")
                        .append(CsvUtils.escape(b.getPersonName())).append(",")
                        .append(CsvUtils.escape(b.getLocation())).append(",")
                        .append(CsvUtils.escape(b.getDescription())).append(",")
                        .append(CsvUtils.escape(String.format(Locale.getDefault(), "%.2f", b.getAmount())))
                        .append("\n");
            }

            // 保存到缓存
            File dir = new File(getCacheDir(), "shares");
            if (!dir.exists()) dir.mkdirs();
            String safeName = personName.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fff]", "");
            File file = new File(dir, "bill_" + safeName + "_" + startDate + ".csv");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();

            Uri shareUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, shareUri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "分享账单表格"));

        } catch (Exception e) {
            Toast.makeText(this, "生成表格失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void expand(final View view) {
        // 用父容器宽度测量，避免 GONE 状态下 getWidth() 为 0 导致测量偏高
        int parentWidth = ((View) view.getParent()).getWidth();
        view.measure(
                View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        final int targetHeight = view.getMeasuredHeight();

        view.setVisibility(View.VISIBLE);
        view.getLayoutParams().height = 0;
        view.requestLayout();

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                view.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.requestLayout();
            }
        });
        animator.start();
    }

    private void collapse(final View view) {
        final int initialHeight = view.getHeight();

        // 动画前锁定为当前固定高度
        view.getLayoutParams().height = initialHeight;
        view.requestLayout();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.setDuration(200);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            view.getLayoutParams().height = (int) animation.getAnimatedValue();
            view.requestLayout();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                view.setVisibility(View.GONE);
            }
        });
        animator.start();
    }

}
