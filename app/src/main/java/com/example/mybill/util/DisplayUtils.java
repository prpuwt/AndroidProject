package com.example.mybill.util;

import android.content.res.Resources;

public final class DisplayUtils {
    private DisplayUtils() {}

    public static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density + 0.5f);
    }
}
