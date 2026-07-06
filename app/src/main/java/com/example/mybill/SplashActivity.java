package com.example.mybill;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mybill.adapter.OnboardingAdapter;
import com.example.mybill.config.Constants;
import com.example.mybill.R;

import java.util.Locale;

public class SplashActivity extends AppCompatActivity {

    private static final String PREFS_NAME = Constants.PREFS_NAME;
    private static final String KEY_HAS_ONBOARDED = Constants.KEY_HAS_ONBOARDED;
    private static final long SPLASH_DELAY_MS = 1000; //闪屏时间

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private TextView btnSkip;
    private TextView btnNext;
    private View onboardingContainer;
    private View splashContainer;
    private ImageView[] dots;
    private final int totalPages = 4;
    private Handler splashHandler;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    @Override
    protected void attachBaseContext(Context newBase) {
        Locale locale = Locale.SIMPLIFIED_CHINESE;
        Locale.setDefault(locale);
        android.content.res.Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        onboardingContainer = findViewById(R.id.onboarding_container);
        splashContainer = findViewById(R.id.splash_container);
        viewPager = findViewById(R.id.view_pager_splash);
        layoutDots = findViewById(R.id.layout_dots);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);

        boolean hasOnboarded = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_HAS_ONBOARDED, false);

        if (hasOnboarded) {
            showSplashMode();
        } else {
            showOnboardingMode();
        }
    }

    private void showSplashMode() {
        splashContainer.setVisibility(View.VISIBLE);
        onboardingContainer.setVisibility(View.GONE);

        splashContainer.startAnimation(
                AnimationUtils.loadAnimation(this, R.anim.splash_fade_in));

        splashHandler = new Handler(Looper.getMainLooper());
        splashHandler.postDelayed(this::navigateToMain, SPLASH_DELAY_MS);
    }

    private void showOnboardingMode() {
        splashContainer.setVisibility(View.GONE);
        onboardingContainer.setVisibility(View.VISIBLE);

        OnboardingAdapter adapter = new OnboardingAdapter();
        viewPager.setAdapter(adapter);
        setupDots();

        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                btnNext.setText(position == totalPages - 1 ? "完成" : "下一步");
            }
        };
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < totalPages - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                completeOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> completeOnboarding());
    }

    private void setupDots() {
        dots = new ImageView[totalPages];
        for (int i = 0; i < totalPages; i++) {
            dots[i] = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            dots[i].setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.dot_unselected));
            layoutDots.addView(dots[i]);
        }
        updateDots(0);
    }

    private void updateDots(int position) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageDrawable(ContextCompat.getDrawable(this,
                    i == position ? R.drawable.dot_selected : R.drawable.dot_unselected));
        }
    }

    private void completeOnboarding() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HAS_ONBOARDED, true)
                .apply();
        navigateToMain();
    }

    private void navigateToMain() {
        boolean hasOnboarded = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_HAS_ONBOARDED, false);
        if (hasOnboarded && LockActivity.isLockEnabled(this)) {
            Intent intent = new Intent(this, LockActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        overridePendingTransition(R.anim.splash_fade_in, R.anim.splash_fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null) {
            splashHandler.removeCallbacksAndMessages(null);
        }
        if (viewPager != null && pageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    @Override
    public void onBackPressed() {
        if (onboardingContainer.getVisibility() == View.VISIBLE
                && viewPager.getCurrentItem() > 0) {
            viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
        } else {
            super.onBackPressed();
        }
    }
}
