package com.example.mybill;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mybill.db.BillDatabaseHelper;
import com.example.mybill.model.Bill;
import com.example.mybill.view.FlowLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class AddBillActivity extends AppCompatActivity {

    private static final String PREFS_DRAFT = "add_bill_draft";
    private static final String KEY_DRAFT = "draft";
    private static final long DRAFT_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000;

    private TextView tvSelectedDate;
    private TextView tvPeopleCount;
    private LinearLayout layoutPeople;
    private FlowLayout layoutLocations;
    private TextInputEditText etDescription;
    private TextInputLayout tilDescription;
    private MaterialButton btnTogglePeople;
    private BillDatabaseHelper dbHelper;

    private String internalDate;
    private List<String> peopleList = new ArrayList<>();
    private List<String> locationList = new ArrayList<>();
    private final List<View> peopleRows = new ArrayList<>();
    private final List<CheckBox> locationCheckboxes = new ArrayList<>();
    private ListPopupWindow amountSuggestionPopup;
    private EditText amountPopupAnchor;
    private boolean draftCompleted;

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
        setContentView(R.layout.activity_add_bill);
        overridePendingTransition(R.anim.slide_in_up, 0);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_add), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,
                    Math.max(systemBars.bottom, ime.bottom));
            return insets;
        });

        dbHelper = new BillDatabaseHelper(this);

        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvPeopleCount = findViewById(R.id.tv_people_count);
        layoutPeople = findViewById(R.id.layout_people);
        layoutLocations = findViewById(R.id.layout_locations);
        etDescription = findViewById(R.id.et_description);
        tilDescription = findViewById(R.id.til_description);
        btnTogglePeople = findViewById(R.id.btn_toggle_people);
        MaterialButton btnSave = findViewById(R.id.btn_save);
        MaterialButton btnBack = findViewById(R.id.btn_back);
        MaterialButton btnAddPerson = findViewById(R.id.btn_add_person);
        MaterialButton btnAddLocation = findViewById(R.id.btn_add_location);
        MaterialButton btnManagePeople = findViewById(R.id.btn_manage_people);
        MaterialButton btnManageLocations = findViewById(R.id.btn_manage_locations);
        MaterialCardView cardDate = findViewById(R.id.card_date);

        // 默认日期：今天
        Calendar today = Calendar.getInstance();
        internalDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH));
        tvSelectedDate.setText(formatDateChinese(internalDate));

        // 日期选择
        cardDate.setOnClickListener(v -> {
            long selectedMillis = parseDateToMillis(internalDate);
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("选择日期")
                    .setSelection(selectedMillis)
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
                cal.setTimeInMillis(selection);
                internalDate = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
                tvSelectedDate.setText(formatDateChinese(internalDate));
            });
            picker.show(getSupportFragmentManager(), "date_picker");
        });

        refreshPeople();
        refreshLocations();

        btnAddPerson.setOnClickListener(v -> showAddPersonDialog());
        btnAddLocation.setOnClickListener(v -> showAddLocationDialog());
        btnManagePeople.setOnClickListener(v -> showManagePeopleDialog());
        btnManageLocations.setOnClickListener(v -> showManageLocationsDialog());
        btnTogglePeople.setOnClickListener(v -> toggleAllPeople());
        btnSave.setOnClickListener(v -> saveBills());
        btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.btn_draft).setOnClickListener(v -> onDraftButtonClicked());

        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                tilDescription.setError(null);
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_out_down);
    }

    // ==================== 人员列表 ====================

    private void refreshPeople() {
        dismissAmountSuggestions();
        Map<String, Boolean> checkedStates = new HashMap<>();
        Map<String, String> amountStates = new HashMap<>();
        for (View row : peopleRows) {
            CheckBox cb = row.findViewById(R.id.cb_person);
            EditText et = row.findViewById(R.id.et_amount);
            String name = cb.getText().toString();
            checkedStates.put(name, cb.isChecked());
            amountStates.put(name, et.getText() != null ? et.getText().toString() : "");
        }

        TransitionManager.beginDelayedTransition(layoutPeople);
        layoutPeople.removeAllViews();
        peopleRows.clear();
        peopleList = dbHelper.getAllPeople();

        for (String name : peopleList) {
            View row = createPersonRow(name);
            EditText et = row.findViewById(R.id.et_amount);
            String amount = amountStates.get(name);
            if (amount != null && !amount.isEmpty()) {
                et.setText(amount);
            }
            CheckBox cb = row.findViewById(R.id.cb_person);
            Boolean wasChecked = checkedStates.get(name);
            if (wasChecked != null) {
                cb.setChecked(wasChecked);
            }
            layoutPeople.addView(row);
            peopleRows.add(row);
        }
        updatePeopleCount();
    }

    private View createPersonRow(String name) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_person_amount, layoutPeople, false);
        CheckBox cb = row.findViewById(R.id.cb_person);
        cb.setText(name);
        TextInputLayout tilAmount = row.findViewById(R.id.til_amount);

        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                tilAmount.setError(null);
            }
            updatePeopleCount();
        });
        row.setOnClickListener(v -> {
            cb.setChecked(!cb.isChecked());
            updatePeopleCount();
        });

        EditText etAmount = row.findViewById(R.id.et_amount);
        etAmount.setOnClickListener(v -> showAmountSuggestions(etAmount));
        etAmount.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.post(() -> showAmountSuggestions(etAmount));
            }
        });
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dismissAmountSuggestions();
                tilAmount.setError(null);
                if (s.length() > 0 && !cb.isChecked()) {
                    cb.setChecked(true);
                }
            }
        });

        return row;
    }

    private void showAmountSuggestions(EditText target) {
        if (amountSuggestionPopup != null && amountSuggestionPopup.isShowing()
                && amountPopupAnchor == target) {
            return;
        }
        List<String> suggestions = collectOtherAmounts(target);
        if (suggestions.isEmpty()) return;

        dismissAmountSuggestions();
        ListPopupWindow popup = new ListPopupWindow(this);
        popup.setAdapter(new AmountSuggestionAdapter(suggestions, target));
        popup.setAnchorView(target);
        popup.setWidth(ListPopupWindow.WRAP_CONTENT);
        popup.setModal(false);
        popup.setInputMethodMode(ListPopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setVerticalOffset(Math.round(4 * getResources().getDisplayMetrics().density));
        popup.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_amount_popup));
        popup.setPromptView(createSuggestionHeader());
        popup.show();
        amountSuggestionPopup = popup;
        amountPopupAnchor = target;
    }

    private View createSuggestionHeader() {
        TextView header = new TextView(this);
        header.setText("其他人已填金额");
        header.setTextSize(12);
        header.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        int hPad = Math.round(16 * getResources().getDisplayMetrics().density);
        int vPad = Math.round(8 * getResources().getDisplayMetrics().density);
        header.setPadding(hPad, vPad, hPad, vPad / 2);
        return header;
    }

    private void fillAmount(EditText target, String amount) {
        target.setText(amount);
        target.setSelection(amount.length());
    }

    private void applyAmountToChecked(String amount) {
        int count = 0;
        for (View row : peopleRows) {
            CheckBox cb = row.findViewById(R.id.cb_person);
            if (!cb.isChecked()) continue;
            EditText et = row.findViewById(R.id.et_amount);
            et.setText(amount);
            et.setSelection(et.getText().length());
            count++;
        }
        dismissAmountSuggestions();
        if (count == 0) {
            Toast.makeText(this, "请先勾选人员", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "已为勾选的 " + count + " 人填入 ¥" + amount,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class AmountSuggestionAdapter extends BaseAdapter {

        private final List<String> amounts;
        private final EditText target;
        private final LayoutInflater inflater = LayoutInflater.from(AddBillActivity.this);

        AmountSuggestionAdapter(List<String> amounts, EditText target) {
            this.amounts = amounts;
            this.target = target;
        }

        @Override
        public int getCount() {
            return amounts.size();
        }

        @Override
        public Object getItem(int position) {
            return amounts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_amount_suggestion, parent, false);
            }
            String amount = amounts.get(position);
            TextView tvAmount = convertView.findViewById(R.id.tv_suggestion_amount);
            TextView tvApplyAll = convertView.findViewById(R.id.tv_apply_all);
            tvAmount.setText("¥ " + amount);
            convertView.setOnClickListener(v -> {
                fillAmount(target, amount);
                dismissAmountSuggestions();
            });
            tvApplyAll.setOnClickListener(v -> applyAmountToChecked(amount));
            return convertView;
        }
    }

    private List<String> collectOtherAmounts(EditText target) {
        List<String> amounts = new ArrayList<>();
        for (View row : peopleRows) {
            EditText et = row.findViewById(R.id.et_amount);
            if (et == target) continue;
            String text = et.getText() != null ? et.getText().toString().trim() : "";
            if (!text.isEmpty() && !amounts.contains(text)) {
                amounts.add(text);
            }
        }
        return amounts;
    }

    private void dismissAmountSuggestions() {
        if (amountSuggestionPopup != null && amountSuggestionPopup.isShowing()) {
            amountSuggestionPopup.dismiss();
        }
        amountSuggestionPopup = null;
        amountPopupAnchor = null;
    }

    @Override
    protected void onStop() {
        dismissAmountSuggestions();
        if (!draftCompleted) {
            saveDraft();
        }
        super.onStop();
    }

    // ==================== 草稿 ====================

    private void saveDraft() {
        try {
            String description = etDescription.getText() != null
                    ? etDescription.getText().toString().trim() : "";

            JSONArray people = new JSONArray();
            boolean hasAmount = false;
            int checkedCount = 0;
            for (int i = 0; i < peopleRows.size(); i++) {
                View row = peopleRows.get(i);
                CheckBox cb = row.findViewById(R.id.cb_person);
                EditText et = row.findViewById(R.id.et_amount);
                String amount = et.getText() != null ? et.getText().toString().trim() : "";
                if (!amount.isEmpty()) hasAmount = true;
                if (cb.isChecked()) checkedCount++;
                JSONObject person = new JSONObject();
                person.put("name", peopleList.get(i));
                person.put("checked", cb.isChecked());
                person.put("amount", amount);
                people.put(person);
            }

            JSONArray locations = new JSONArray();
            for (CheckBox cb : locationCheckboxes) {
                if (cb.isChecked()) {
                    locations.put(cb.getText().toString());
                }
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_DRAFT, MODE_PRIVATE);
            boolean untouched = description.isEmpty() && !hasAmount
                    && checkedCount == 0 && locations.length() == 0;
            if (untouched) {
                prefs.edit().remove(KEY_DRAFT).apply();
                return;
            }

            JSONObject draft = new JSONObject();
            draft.put("date", internalDate);
            draft.put("description", description);
            draft.put("locations", locations);
            draft.put("people", people);
            draft.put("ts", System.currentTimeMillis());
            prefs.edit().putString(KEY_DRAFT, draft.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private boolean restoreDraft() {
        SharedPreferences prefs = getSharedPreferences(PREFS_DRAFT, MODE_PRIVATE);
        String raw = prefs.getString(KEY_DRAFT, null);
        if (raw == null) return false;

        try {
            JSONObject draft = new JSONObject(raw);
            long ts = draft.optLong("ts", 0);
            if (System.currentTimeMillis() - ts > DRAFT_MAX_AGE_MS) {
                clearDraft();
                return false;
            }

            internalDate = draft.optString("date", internalDate);
            tvSelectedDate.setText(formatDateChinese(internalDate));

            String description = draft.optString("description", "");
            etDescription.setText(description);

            JSONArray locations = draft.optJSONArray("locations");
            if (locations != null) {
                Set<String> selected = new HashSet<>();
                for (int i = 0; i < locations.length(); i++) {
                    selected.add(locations.optString(i));
                }
                for (CheckBox cb : locationCheckboxes) {
                    cb.setChecked(selected.contains(cb.getText().toString()));
                }
            }

            JSONArray people = draft.optJSONArray("people");
            if (people != null) {
                Map<String, JSONObject> byName = new HashMap<>();
                for (int i = 0; i < people.length(); i++) {
                    JSONObject person = people.optJSONObject(i);
                    if (person != null) {
                        byName.put(person.optString("name"), person);
                    }
                }
                for (int i = 0; i < peopleRows.size(); i++) {
                    View row = peopleRows.get(i);
                    EditText et = row.findViewById(R.id.et_amount);
                    CheckBox cb = row.findViewById(R.id.cb_person);
                    et.setText("");
                    cb.setChecked(false);
                    JSONObject person = byName.get(peopleList.get(i));
                    if (person == null) continue;
                    String amount = person.optString("amount", "");
                    if (!amount.isEmpty()) {
                        et.setText(amount);
                    }
                    cb.setChecked(person.optBoolean("checked", false));
                }
                updatePeopleCount();
            }
            return true;
        } catch (Exception e) {
            clearDraft();
            return false;
        }
    }

    private void clearDraft() {
        getSharedPreferences(PREFS_DRAFT, MODE_PRIVATE)
                .edit().remove(KEY_DRAFT).apply();
    }

    private void onDraftButtonClicked() {
        SharedPreferences prefs = getSharedPreferences(PREFS_DRAFT, MODE_PRIVATE);
        if (!prefs.contains(KEY_DRAFT)) {
            Toast.makeText(this, "暂无草稿", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isFormEmpty()) {
            loadDraft();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("恢复草稿")
                .setMessage("当前已填写的内容将被覆盖，确定恢复上次草稿吗？")
                .setPositiveButton("恢复", (d, w) -> loadDraft())
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadDraft() {
        if (restoreDraft()) {
            Toast.makeText(this, "已恢复上次未保存的内容", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "暂无草稿", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isFormEmpty() {
        String description = etDescription.getText() != null
                ? etDescription.getText().toString().trim() : "";
        if (!description.isEmpty()) return false;

        for (View row : peopleRows) {
            CheckBox cb = row.findViewById(R.id.cb_person);
            EditText et = row.findViewById(R.id.et_amount);
            if (cb.isChecked()) return false;
            if (et.getText() != null && et.getText().toString().trim().length() > 0) return false;
        }
        for (CheckBox cb : locationCheckboxes) {
            if (cb.isChecked()) return false;
        }
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View focused = getCurrentFocus();
            if (focused instanceof EditText && !isTouchOnInput(ev)) {
                focused.clearFocus();
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isTouchOnInput(MotionEvent ev) {
        float x = ev.getRawX();
        float y = ev.getRawY();
        if (isPointInside(x, y, etDescription)) return true;
        for (View row : peopleRows) {
            EditText et = row.findViewById(R.id.et_amount);
            if (isPointInside(x, y, et)) return true;
        }
        return false;
    }

    private boolean isPointInside(float x, float y, View view) {
        if (view == null) return false;
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect)) return false;
        return rect.contains((int) x, (int) y);
    }

    private void showManagePeopleDialog() {
        List<String> people = dbHelper.getAllPeople();
        if (people.isEmpty()) {
            Toast.makeText(this, "暂无人员", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean[] checked = new boolean[people.size()];

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_list, null);
        LinearLayout layoutCheckboxList = dialogView.findViewById(R.id.layout_checkbox_list);

        for (int i = 0; i < people.size(); i++) {
            String name = people.get(i);
            int billCount = dbHelper.countBillsByPerson(name);
            CheckBox cb = (CheckBox) LayoutInflater.from(this)
                    .inflate(R.layout.item_location_checkbox, layoutCheckboxList, false);
            cb.setText(name + "（" + billCount + "条记录）");
            int index = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> checked[index] = isChecked);
            layoutCheckboxList.addView(cb);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("删除人员（可多选）")
                .setView(dialogView)
                .setPositiveButton("删除", (d, which) -> {
                    int deleted = 0;
                    for (int i = 0; i < people.size(); i++) {
                        if (checked[i]) {
                            dbHelper.deletePersonByName(people.get(i));
                            deleted++;
                        }
                    }
                    if (deleted > 0) {
                        refreshPeople();
                        Toast.makeText(this, "已删除" + deleted + "人，账单记录已保留", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updatePeopleCount() {
        int count = 0;
        for (View row : peopleRows) {
            CheckBox cb = row.findViewById(R.id.cb_person);
            if (cb.isChecked()) count++;
        }
        tvPeopleCount.setText(String.format(Locale.getDefault(), "已选%d人", count));
        if (btnTogglePeople != null) {
            boolean allSelected = !peopleRows.isEmpty() && count == peopleRows.size();
            btnTogglePeople.setText(allSelected ? "清空" : "全选");
            btnTogglePeople.setEnabled(!peopleRows.isEmpty());
        }
    }

    private void toggleAllPeople() {
        if (peopleRows.isEmpty()) return;
        boolean allSelected = true;
        for (View row : peopleRows) {
            CheckBox cb = row.findViewById(R.id.cb_person);
            if (!cb.isChecked()) {
                allSelected = false;
                break;
            }
        }
        boolean target = !allSelected;
        for (View row : peopleRows) {
            CheckBox cb = row.findViewById(R.id.cb_person);
            cb.setChecked(target);
        }
        updatePeopleCount();
    }

    private void showManageLocationsDialog() {
        List<String> locations = dbHelper.getAllLocations();
        if (locations.isEmpty()) {
            Toast.makeText(this, "暂无地点", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean[] checked = new boolean[locations.size()];

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manage_list, null);
        LinearLayout layoutCheckboxList = dialogView.findViewById(R.id.layout_checkbox_list);

        for (int i = 0; i < locations.size(); i++) {
            String name = locations.get(i);
            int billCount = dbHelper.countBillsByLocation(name);
            CheckBox cb = (CheckBox) LayoutInflater.from(this)
                    .inflate(R.layout.item_location_checkbox, layoutCheckboxList, false);
            cb.setText(name + "（" + billCount + "条记录）");
            int index = i;
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> checked[index] = isChecked);
            layoutCheckboxList.addView(cb);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("删除地点（可多选）")
                .setView(dialogView)
                .setPositiveButton("删除", (d, which) -> {
                    int deleted = 0;
                    for (int i = 0; i < locations.size(); i++) {
                        if (checked[i]) {
                            dbHelper.deleteLocationByName(locations.get(i));
                            deleted++;
                        }
                    }
                    if (deleted > 0) {
                        refreshLocations();
                        Toast.makeText(this, "已删除" + deleted + "个地点，账单记录已保留", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAddPersonDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_person, null);
        EditText etName = dialogView.findViewById(R.id.et_person_name);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("确定", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        long id = dbHelper.insertPerson(name);
                        if (id != -1) {
                            refreshPeople();
                        } else {
                            Toast.makeText(this, "该人员已存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 地点（可多选） ====================

    private void refreshLocations() {
        dismissAmountSuggestions();
        Set<String> selected = new HashSet<>();
        for (CheckBox cb : locationCheckboxes) {
            if (cb.isChecked()) {
                selected.add(cb.getText().toString());
            }
        }

        layoutLocations.removeAllViews();
        locationCheckboxes.clear();
        locationList = dbHelper.getAllLocations();

        for (String loc : locationList) {
            CheckBox cb = (CheckBox) LayoutInflater.from(this)
                    .inflate(R.layout.item_location_checkbox, layoutLocations, false);
            cb.setText(loc);
            cb.setChecked(selected.contains(loc));
            layoutLocations.addView(cb);
            locationCheckboxes.add(cb);
        }
    }

    private void showAddLocationDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_location, null);
        EditText etName = dialogView.findViewById(R.id.et_location_name);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("确定", (d, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        long id = dbHelper.insertLocation(name);
                        if (id != -1) {
                            refreshLocations();
                        } else {
                            Toast.makeText(this, "该地点已存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 保存 ====================

    private void saveBills() {
        dismissAmountSuggestions();
        tilDescription.setError(null);
        for (View row : peopleRows) {
            ((TextInputLayout) row.findViewById(R.id.til_amount)).setError(null);
        }

        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        if (description.isEmpty()) {
            tilDescription.setError(getString(R.string.description_required));
            etDescription.requestFocus();
            return;
        }

        // 获取选中的地点
        List<String> selectedLocations = new ArrayList<>();
        for (CheckBox cb : locationCheckboxes) {
            if (cb.isChecked()) {
                selectedLocations.add(cb.getText().toString());
            }
        }
        if (selectedLocations.isEmpty()) {
            Toast.makeText(this, getString(R.string.location_required), Toast.LENGTH_SHORT).show();
            return;
        }
        String location = joinNames(selectedLocations);

        // 收集选中的人员和金额
        List<Bill> bills = new ArrayList<>();
        for (int i = 0; i < peopleRows.size(); i++) {
            View row = peopleRows.get(i);
            CheckBox cb = row.findViewById(R.id.cb_person);
            EditText etAmount = row.findViewById(R.id.et_amount);

            if (cb.isChecked()) {
                String personName = peopleList.get(i);
                String amountStr = etAmount.getText().toString().trim();
                TextInputLayout tilAmount = row.findViewById(R.id.til_amount);
                if (amountStr.isEmpty()) {
                    tilAmount.setError("请输入金额");
                    etAmount.requestFocus();
                    return;
                }
                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    tilAmount.setError("金额格式不正确");
                    etAmount.requestFocus();
                    return;
                }
                if (amount <= 0) {
                    tilAmount.setError("金额必须大于0");
                    etAmount.requestFocus();
                    return;
                }
                bills.add(new Bill(internalDate, personName, location, description, amount));
            }
        }

        if (bills.isEmpty()) {
            Toast.makeText(this, getString(R.string.person_required), Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查同一天是否已有该人员的账单
        List<String> duplicateNames = new ArrayList<>();
        try {
            String[] parts = internalDate.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            List<Bill> todayBills = dbHelper.getBillsByDay(year, month, day);
            for (Bill bill : bills) {
                for (Bill existing : todayBills) {
                    if (existing.getPersonName().equals(bill.getPersonName())) {
                        duplicateNames.add(bill.getPersonName());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        if (!duplicateNames.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("重复提醒")
                    .setMessage(internalDate + "\n\n"
                            + android.text.TextUtils.join("、", duplicateNames)
                            + " 在今天已有账单，是否继续添加？")
                    .setPositiveButton("继续添加", (d, w) -> {
                        dbHelper.insertBills(bills);
                        clearDraft();
                        draftCompleted = true;
                        Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            dbHelper.insertBills(bills);
            clearDraft();
            draftCompleted = true;
            Toast.makeText(this, getString(R.string.save_success), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ==================== 工具 ====================

    private String joinNames(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(names.get(i));
        }
        return sb.toString();
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
}
