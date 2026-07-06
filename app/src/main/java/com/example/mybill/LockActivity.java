package com.example.mybill;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mybill.config.Constants;
import com.example.mybill.util.CryptoUtils;
import com.example.mybill.util.AnimationUtils;
import com.example.mybill.util.DisplayUtils;
import com.example.mybill.view.PatternLockView;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class LockActivity extends AppCompatActivity {

    private static final String LOCK_PREFS = Constants.PREFS_LOCK;
    static final String KEY_LOCK_PATTERN = Constants.KEY_LOCK_PATTERN;
    static final String KEY_LOCK_PIN = Constants.KEY_LOCK_PIN;

    private PatternLockView patternLock;
    private LinearLayout layoutPin;
    private LinearLayout layoutPinDots;
    private TextView tvLockHint, tvPinMessage;
    private ImageView ivLockIcon;
    private MaterialButton btnSwitchMode;

    private String lockType = "pattern"; // 当前显示的解锁方式
    private boolean hasPattern, hasPin;
    private StringBuilder pinInput = new StringBuilder();
    private int maxPinLength = 6;
    private int errorCount = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean verifying = false;

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
        setContentView(R.layout.activity_lock);

        SharedPreferences prefs = getSharedPreferences(LOCK_PREFS, MODE_PRIVATE);
        hasPattern = !prefs.getString(KEY_LOCK_PATTERN, "").isEmpty();
        hasPin = !prefs.getString(KEY_LOCK_PIN, "").isEmpty();

        if (!hasPattern && !hasPin) {
            navigateToMain();
            return;
        }

        // 默认显示有凭据的方式；两种都有时优先图案
        lockType = hasPattern ? "pattern" : "pin";

        patternLock = findViewById(R.id.pattern_lock);
        layoutPin = findViewById(R.id.layout_pin);
        layoutPinDots = findViewById(R.id.layout_pin_dots);
        tvLockHint = findViewById(R.id.tv_lock_hint);
        tvPinMessage = findViewById(R.id.tv_pin_message);
        ivLockIcon = findViewById(R.id.iv_lock_icon);
        btnSwitchMode = findViewById(R.id.btn_switch_mode);

        MaterialButton btn0 = findViewById(R.id.btn_num_0);
        MaterialButton btn1 = findViewById(R.id.btn_num_1);
        MaterialButton btn2 = findViewById(R.id.btn_num_2);
        MaterialButton btn3 = findViewById(R.id.btn_num_3);
        MaterialButton btn4 = findViewById(R.id.btn_num_4);
        MaterialButton btn5 = findViewById(R.id.btn_num_5);
        MaterialButton btn6 = findViewById(R.id.btn_num_6);
        MaterialButton btn7 = findViewById(R.id.btn_num_7);
        MaterialButton btn8 = findViewById(R.id.btn_num_8);
        MaterialButton btn9 = findViewById(R.id.btn_num_9);
        MaterialButton btnDel = findViewById(R.id.btn_num_del);

        View.OnClickListener numListener = v -> {
            if (verifying) return;
            String tag = (String) v.getTag();
            if (pinInput.length() < maxPinLength) {
                pinInput.append(tag);
                updatePinDots();
                if (pinInput.length() == maxPinLength) {
                    verifyPin();
                }
            }
        };

        btn0.setTag("0"); btn0.setOnClickListener(numListener);
        btn1.setTag("1"); btn1.setOnClickListener(numListener);
        btn2.setTag("2"); btn2.setOnClickListener(numListener);
        btn3.setTag("3"); btn3.setOnClickListener(numListener);
        btn4.setTag("4"); btn4.setOnClickListener(numListener);
        btn5.setTag("5"); btn5.setOnClickListener(numListener);
        btn6.setTag("6"); btn6.setOnClickListener(numListener);
        btn7.setTag("7"); btn7.setOnClickListener(numListener);
        btn8.setTag("8"); btn8.setOnClickListener(numListener);
        btn9.setTag("9"); btn9.setOnClickListener(numListener);

        btnDel.setOnClickListener(v -> {
            if (pinInput.length() > 0) {
                pinInput.deleteCharAt(pinInput.length() - 1);
                updatePinDots();
            }
        });

        btnSwitchMode.setOnClickListener(v -> {
            lockType = "pattern".equals(lockType) ? "pin" : "pattern";
            updateLockUI();
        });

        patternLock.setOnPatternListener(this::onPatternDrawn);
        updateLockUI();
    }

    private void updateLockUI() {
        boolean both = hasPattern && hasPin;
        btnSwitchMode.setVisibility(both ? View.VISIBLE : View.GONE);

        if ("pattern".equals(lockType)) {
            patternLock.setVisibility(View.VISIBLE);
            layoutPin.setVisibility(View.GONE);
            tvLockHint.setText("请绘制解锁图案");
            if (both) btnSwitchMode.setText("切换到密码解锁");
        } else {
            patternLock.setVisibility(View.GONE);
            layoutPin.setVisibility(View.VISIBLE);
            tvLockHint.setText("请输入密码");
            if (both) btnSwitchMode.setText("切换到图案解锁");
            pinInput.setLength(0);
            tvPinMessage.setVisibility(View.GONE);
            updatePinDots();
        }
    }

    private void updatePinDots() {
        layoutPinDots.removeAllViews();
        for (int i = 0; i < maxPinLength; i++) {
            View dot = new View(this);
            int size = DisplayUtils.dpToPx(14);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(DisplayUtils.dpToPx(8), 0, DisplayUtils.dpToPx(8), 0);
            dot.setLayoutParams(params);
            boolean selected = i < pinInput.length();
            dot.setBackgroundResource(selected
                    ? R.drawable.dot_selected : R.drawable.dot_unselected);
            layoutPinDots.addView(dot);
            if (selected && i == pinInput.length() - 1) {
                AnimationUtils.popIn(dot);
            }
        }
    }

    private void onPatternDrawn(String pattern) {
        if (verifying) return;
        verifying = true;

        SharedPreferences prefs = getSharedPreferences(LOCK_PREFS, MODE_PRIVATE);
        String savedHash = prefs.getString(KEY_LOCK_PATTERN, "");

        if (CryptoUtils.sha256(pattern).equals(savedHash)) {
            patternLock.setStatus(PatternLockView.STATUS_CORRECT);
            handler.postDelayed(this::onUnlockSuccess, 400);
        } else {
            patternLock.setStatus(PatternLockView.STATUS_ERROR);
            patternLock.shake();
            errorCount++;
            tvLockHint.setText("图案错误，请重试 (" + errorCount + ")");
            handler.postDelayed(() -> {
                patternLock.clearPattern();
                verifying = false;
            }, 800);
        }
    }

    private void verifyPin() {
        if (verifying) return;
        verifying = true;

        SharedPreferences prefs = getSharedPreferences(LOCK_PREFS, MODE_PRIVATE);
        String savedHash = prefs.getString(KEY_LOCK_PIN, "");

        if (CryptoUtils.sha256(pinInput.toString()).equals(savedHash)) {
            tvPinMessage.setVisibility(View.GONE);
            handler.postDelayed(this::onUnlockSuccess, 300);
        } else {
            errorCount++;
            tvPinMessage.setText("密码错误，请重试 (" + errorCount + "/5)");
            tvPinMessage.setVisibility(View.VISIBLE);
            pinInput.setLength(0);
            updatePinDots();
            handler.postDelayed(() -> verifying = false, 500);

            if (errorCount >= 5) {
                handler.postDelayed(this::finish, 2000);
            }
        }
    }

    private void onUnlockSuccess() {
        navigateToMain();
    }

    private void navigateToMain() {
        String target = getIntent().getStringExtra(Constants.EXTRA_TARGET_ACTIVITY);

        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);

        if (Constants.TARGET_ADD_BILL.equals(target)) {
            Intent addIntent = new Intent(this, AddBillActivity.class);
            startActivity(addIntent);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        // 不允许返回跳过锁屏
    }

    public static boolean isLockEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LOCK_PREFS, Context.MODE_PRIVATE);
        return !prefs.getString(KEY_LOCK_PATTERN, "").isEmpty()
                || !prefs.getString(KEY_LOCK_PIN, "").isEmpty();
    }
}
