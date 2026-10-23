package com.example.mybill.util;

import android.graphics.Bitmap;

public final class BlurUtils {

    private BlurUtils() {
    }

    // 三次盒式模糊，近似高斯，只处理不透明像素
    public static void boxBlur(Bitmap bitmap, int radius) {
        if (radius < 1) return;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] src = new int[w * h];
        int[] dst = new int[w * h];
        bitmap.getPixels(src, 0, w, 0, 0, w, h);

        for (int pass = 0; pass < 3; pass++) {
            blurPass(src, dst, w, h, radius, true);
            blurPass(dst, src, w, h, radius, false);
        }
        bitmap.setPixels(src, 0, w, 0, 0, w, h);
    }

    private static void blurPass(int[] src, int[] dst, int w, int h, int radius, boolean horizontal) {
        int length = horizontal ? w : h;
        int lines = horizontal ? h : w;
        int div = radius * 2 + 1;

        for (int line = 0; line < lines; line++) {
            int sumR = 0, sumG = 0, sumB = 0;
            for (int i = -radius; i <= radius; i++) {
                int c = src[index(horizontal, line, clamp(i, length), w)];
                sumR += (c >> 16) & 0xFF;
                sumG += (c >> 8) & 0xFF;
                sumB += c & 0xFF;
            }
            for (int i = 0; i < length; i++) {
                dst[index(horizontal, line, i, w)] =
                        (sumR / div << 16) | (sumG / div << 8) | (sumB / div) | 0xFF000000;
                int add = src[index(horizontal, line, Math.min(i + radius + 1, length - 1), w)];
                int sub = src[index(horizontal, line, Math.max(i - radius, 0), w)];
                sumR += ((add >> 16) & 0xFF) - ((sub >> 16) & 0xFF);
                sumG += ((add >> 8) & 0xFF) - ((sub >> 8) & 0xFF);
                sumB += (add & 0xFF) - (sub & 0xFF);
            }
        }
    }

    private static int index(boolean horizontal, int line, int i, int w) {
        return horizontal ? line * w + i : i * w + line;
    }

    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(value, length - 1));
    }
}
