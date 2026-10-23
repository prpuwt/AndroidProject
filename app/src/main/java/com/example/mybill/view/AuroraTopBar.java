package com.example.mybill.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.example.mybill.util.BlurUtils;

public class AuroraTopBar extends LinearLayout {

    // 离屏图缩放：值越大越朦胧、越省性能；越小越细腻但更耗性能
    private static final int AURORA_SCALE = 8;
    // 模糊强度：低清图高度的倍数，越大越云雾、色块越融合
    private static final float BLUR_RADIUS_RATIO = 0.25f;

    // 每行一个光斑：{centerX, centerY, radius(×顶栏高度)}
    // centerX/centerY 为 0-1 比例，可超出边界（如 -0.1 / 1.05）
    private static final float[][] BLOB_SPECS = {
            {0.05f, 0.10f, 1.8f},
            {0.28f, 0.95f, 1.6f},
            {0.52f, 0.05f, 1.7f},
            {0.74f, 0.95f, 1.7f},
            {0.95f, 0.10f, 1.6f},
            {0.45f, 0.55f, 2.0f},
    };
    // 与 BLOB_SPECS 一一对应；ARGB 前两位是透明度：00 全透明 ~ FF 不透明
    private static final int[] BLOB_COLORS = {
            0x338CDCC8, 0x2E9EE5D4, 0x2689DCC9,
            0x2E7FD6C1, 0x40A6E8D8, 0x1FFFFFFF,
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path wavePath = new Path();
    private Bitmap auroraBitmap;

    public AuroraTopBar(Context context) {
        super(context);
        init();
    }

    public AuroraTopBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraTopBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        wavePaint.setColor(0xFFDBE9EC);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        if (auroraBitmap != null) auroraBitmap.recycle();
        auroraBitmap = buildAurora(w, h);
    }

    private Bitmap buildAurora(int w, int h) {
        int bw = Math.max(48, w / AURORA_SCALE);
        int bh = Math.max(12, h / AURORA_SCALE);
        Bitmap bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setShader(new LinearGradient(0, 0, bw, bh,
                0xFF2E8775, 0xFF3E9C87, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, bw, bh, base);

        Paint blob = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < BLOB_SPECS.length; i++) {
            float[] spec = BLOB_SPECS[i];
            blob.setColor(BLOB_COLORS[i]);
            canvas.drawCircle(spec[0] * bw, spec[1] * bh, spec[2] * bh, blob);
        }

        BlurUtils.boxBlur(bitmap, Math.max(2, Math.round(bh * BLUR_RADIUS_RATIO)));
        return bitmap;
    }

    private void drawWave(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        float top = h * 0.86f;

        wavePath.reset();
        wavePath.moveTo(0, h);
        wavePath.lineTo(0, top);
        wavePath.cubicTo(w * 0.20f, top - h * 0.18f,
                w * 0.45f, top + h * 0.10f,
                w * 0.62f, top - h * 0.02f);
        wavePath.cubicTo(w * 0.80f, top - h * 0.14f,
                w * 0.90f, top + h * 0.06f,
                w, top - h * 0.02f);
        wavePath.lineTo(w, h);
        wavePath.close();
        canvas.drawPath(wavePath, wavePaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (auroraBitmap == null) {
            super.onDraw(canvas);
            return;
        }
        canvas.drawBitmap(auroraBitmap, null,
                new Rect(0, 0, getWidth(), getHeight()), paint);
        drawWave(canvas);
        super.onDraw(canvas);
    }
}
