package com.example.mybill.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.example.mybill.util.BlurUtils;

public class AuroraRootLayout extends LinearLayout {

    private static final int AURORA_SCALE = 12;
    private static final float BLUR_RADIUS_RATIO = 0.08f;

    // {centerX(宽比例), centerY(高比例), radius(×长边)}
    private static final float[][] BLOB_SPECS = {
            {0.08f, 0.10f, 0.42f},
            {0.92f, 0.22f, 0.38f},
            {0.72f, 0.50f, 0.36f},
            {0.10f, 0.48f, 0.32f},
            {0.95f, 0.90f, 0.38f},
            {0.30f, 0.92f, 0.32f},
            {0.58f, 0.55f, 0.24f},
    };
    private static final int[] BLOB_COLORS = {
            0xC6C6EDE3, 0xB3D1EBFA, 0xA6E3DEF5, 0xB3D6EEFA,
            0xFFA9DDE9, 0xB3D7F3E6, 0x4DFFFFFF,
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap auroraBitmap;

    public AuroraRootLayout(Context context) {
        super(context);
        init();
    }

    public AuroraRootLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AuroraRootLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
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
        int bh = Math.max(48, h / AURORA_SCALE);
        Bitmap bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
        base.setShader(new LinearGradient(0, 0, 0, bh,
                0xFFF8FCFB, 0xFFF3FAFA, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, bw, bh, base);

        float maxDim = Math.max(bw, bh);
        Paint blob = new Paint(Paint.ANTI_ALIAS_FLAG);
        for (int i = 0; i < BLOB_SPECS.length; i++) {
            float[] spec = BLOB_SPECS[i];
            blob.setColor(BLOB_COLORS[i]);
            canvas.drawCircle(spec[0] * bw, spec[1] * bh, spec[2] * maxDim, blob);
        }

        BlurUtils.boxBlur(bitmap, Math.max(3, Math.round(bh * BLUR_RADIUS_RATIO)));
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (auroraBitmap == null) {
            super.onDraw(canvas);
            return;
        }
        canvas.drawBitmap(auroraBitmap, null,
                new Rect(0, 0, getWidth(), getHeight()), paint);
        super.onDraw(canvas);
    }
}
