package com.example.mybill.util;

import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public final class AnimationUtils {
    private AnimationUtils() {}

    public static void applyPressScale(View view) {
        applyPressScale(view, 0.96f);
    }

    public static void applyPressScale(View view, float scaleTo) {
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(scaleTo).scaleY(scaleTo)
                            .setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(100).start();
                    break;
            }
            return false;
        });
    }

    public static void popIn(View view) {
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate()
                .scaleX(1f).scaleY(1f)
                .setDuration(200)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }
}
