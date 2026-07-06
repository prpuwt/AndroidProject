package com.example.mybill.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PatternLockView extends View {

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_CORRECT = 1;
    public static final int STATUS_ERROR = 2;

    private final PointF[] nodes = new PointF[9];
    private final List<Integer> selectedNodes = new ArrayList<>();
    private final List<PointF> touchPath = new ArrayList<>();

    private float nodeRadius;
    private float padding;
    private float nodeX, nodeY, spacing;

    private int status = STATUS_NORMAL;
    private int normalColor = 0xFFBDBDBD;
    private int selectedColor = 0xFF00897B;
    private int errorColor = 0xFFF44336;

    private final Paint nodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private OnPatternListener listener;
    private int minNodes = 4;
    private boolean touchInProgress = false;
    private PointF currentTouch = new PointF();

    public interface OnPatternListener {
        void onPatternComplete(String pattern);
    }

    public PatternLockView(Context context) {
        super(context);
        init(null);
    }

    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PatternLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            android.content.res.TypedArray a = getContext().obtainStyledAttributes(attrs,
                    new int[]{android.R.attr.colorAccent});
            int accent = a.getColor(0, 0xFF00897B);
            selectedColor = accent;
            a.recycle();
        }

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dpToPx(4));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        nodePaint.setStyle(Paint.Style.FILL);

        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setColor(0xFFFFFFFF);
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    public void setMinNodes(int min) {
        this.minNodes = min;
    }

    public void setStatus(int status) {
        this.status = status;
        applyStatusColors();
        invalidate();
    }

    public String getPatternString() {
        StringBuilder sb = new StringBuilder();
        for (int i : selectedNodes) sb.append(i + 1);
        return sb.toString();
    }

    public void shake() {
        this.animate()
                .translationXBy(dpToPx(10))
                .setDuration(50)
                .withEndAction(() -> this.animate()
                        .translationXBy(-dpToPx(20))
                        .setDuration(50)
                        .withEndAction(() -> this.animate()
                                .translationXBy(dpToPx(10))
                                .setDuration(50)
                                .start())
                        .start())
                .start();
    }

    public void clearPattern() {
        selectedNodes.clear();
        touchPath.clear();
        status = STATUS_NORMAL;
        applyStatusColors();
        invalidate();
    }

    private void applyStatusColors() {
        switch (status) {
            case STATUS_CORRECT:
                nodePaint.setColor(0xFF4CAF50);
                linePaint.setColor(0xFF4CAF50);
                break;
            case STATUS_ERROR:
                nodePaint.setColor(errorColor);
                linePaint.setColor(errorColor);
                break;
            default:
                nodePaint.setColor(normalColor);
                linePaint.setColor(selectedColor);
                break;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float usable = Math.min(w, h) - dpToPx(48) * 2;
        nodeX = (w - usable) / 2f;
        nodeY = (h - usable) / 2f;
        spacing = usable / 2f;
        nodeRadius = spacing / 4f;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                nodes[i * 3 + j] = new PointF(
                        nodeX + j * spacing,
                        nodeY + i * spacing);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawLines(canvas);
        drawNodes(canvas);
    }

    private void drawLines(Canvas canvas) {
        // 已选择的路径
        if (selectedNodes.size() >= 2) {
            for (int i = 0; i < selectedNodes.size() - 1; i++) {
                PointF a = nodes[selectedNodes.get(i)];
                PointF b = nodes[selectedNodes.get(i + 1)];
                canvas.drawLine(a.x, a.y, b.x, b.y, linePaint);
            }
        }
        // 当前拖动的线
        if (touchInProgress && !selectedNodes.isEmpty()) {
            PointF last = nodes[selectedNodes.get(selectedNodes.size() - 1)];
            canvas.drawLine(last.x, last.y, currentTouch.x, currentTouch.y, linePaint);
        }
    }

    private void drawNodes(Canvas canvas) {
        for (int i = 0; i < 9; i++) {
            PointF n = nodes[i];
            boolean selected = selectedNodes.contains(i);

            // 外圈
            if (status == STATUS_NORMAL) {
                if (selected) {
                    nodePaint.setColor(selectedColor);
                } else {
                    nodePaint.setColor(normalColor);
                }
            }
            // error/correct 状态已由 applyStatusColors 设置
            canvas.drawCircle(n.x, n.y, nodeRadius, nodePaint);

            // 内圈（选中时变小）
            if (selected) {
                innerPaint.setAlpha(255);
            } else {
                innerPaint.setAlpha(100);
            }
            canvas.drawCircle(n.x, n.y, nodeRadius * 0.35f, innerPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (status == STATUS_CORRECT || status == STATUS_ERROR) return true;

        float x = event.getX();
        float y = event.getY();
        currentTouch.set(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                touchInProgress = true;
                int hit = hitTest(x, y);
                if (hit >= 0 && !selectedNodes.contains(hit)) {
                    selectedNodes.add(hit);
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchInProgress = false;
                if (selectedNodes.size() >= minNodes && listener != null) {
                    listener.onPatternComplete(getPatternString());
                } else if (selectedNodes.size() < minNodes) {
                    // 节点不够，闪烁提示
                    setStatus(STATUS_ERROR);
                    handler.postDelayed(this::clearPattern, 600);
                }
                invalidate();
                break;
        }
        return true;
    }

    private int hitTest(float x, float y) {
        for (int i = 0; i < 9; i++) {
            PointF n = nodes[i];
            float dx = x - n.x;
            float dy = y - n.y;
            if (Math.sqrt(dx * dx + dy * dy) < nodeRadius * 1.5f) {
                return i;
            }
        }
        return -1;
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
