package com.example.mybill;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.mybill.util.DisplayUtils;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class CoachMarkOverlay extends FrameLayout {

    public static class CoachMarkStep {
        public int targetViewId;  // 0 = no hole, full overlay
        public int titleResId;
        public int descResId;
        public boolean isLastStep;

        public CoachMarkStep(int targetViewId, int titleResId, int descResId, boolean isLastStep) {
            this.targetViewId = targetViewId;
            this.titleResId = titleResId;
            this.descResId = descResId;
            this.isLastStep = isLastStep;
        }
    }

    private List<CoachMarkStep> steps;
    private int currentStepIndex = -1;
    private CoachMarkStep currentStep;
    private View targetView;
    private Runnable onDismiss;
    private final Paint overlayPaint;
    private boolean dismissing;

    public CoachMarkOverlay(Context context) {
        super(context);
        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);

        overlayPaint = new Paint();
        overlayPaint.setColor(0xCC000000); // 80% opacity black

        setAlpha(0f);
    }

    public static void startGuide(Activity activity, List<CoachMarkStep> steps, Runnable onDismiss) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        CoachMarkOverlay overlay = new CoachMarkOverlay(activity);
        overlay.steps = steps;
        overlay.onDismiss = onDismiss;
        root.addView(overlay);

        overlay.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(() -> overlay.showStep(0))
                .start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();

        if (currentStep != null && currentStep.targetViewId != 0 && targetView != null) {
            Rect holeRect = getHoleRect();
            if (holeRect != null && !holeRect.isEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas.clipOutRect(holeRect);
                } else {
                    canvas.clipRect(holeRect, android.graphics.Region.Op.DIFFERENCE);
                }
            }
        }

        canvas.drawColor(0xCC000000);
        canvas.restore();
    }

    private Rect getHoleRect() {
        if (targetView == null) return null;

        int[] targetLoc = new int[2];
        targetView.getLocationOnScreen(targetLoc);

        int[] overlayLoc = new int[2];
        getLocationOnScreen(overlayLoc);

        int padding = DisplayUtils.dpToPx(8);
        Rect rect = new Rect(
                targetLoc[0] - overlayLoc[0] - padding,
                targetLoc[1] - overlayLoc[1] - padding,
                targetLoc[0] - overlayLoc[0] + targetView.getWidth() + padding,
                targetLoc[1] - overlayLoc[1] + targetView.getHeight() + padding
        );

        // 步骤1 洞高度缩小为 70%，步骤2 保持原大小，其余缩小为 80%
        if (currentStepIndex == 0) {
            int shrink = (rect.bottom - rect.top) * 3 / 20;
            rect.top += shrink;
            rect.bottom -= shrink;
        } else if (currentStepIndex != 1) {
            int shrink = (rect.bottom - rect.top) / 10;
            rect.top += shrink;
            rect.bottom -= shrink;
        }
        return rect;
    }

    private void showStep(int index) {
        if (dismissing || steps == null || index >= steps.size()) return;

        currentStepIndex = index;
        currentStep = steps.get(index);

        removeAllViews();

        if (currentStep.targetViewId != 0) {
            targetView = ((Activity) getContext()).findViewById(currentStep.targetViewId);
            if (targetView == null) {
                advanceOrDismiss();
                return;
            }
        } else {
            targetView = null;
        }
        invalidate();

        // Inflate tooltip
        View tooltip = LayoutInflater.from(getContext())
                .inflate(R.layout.coach_mark_tooltip, this, false);
        updateTooltipContent(tooltip);
        addView(tooltip);
        tooltip.post(() -> positionTooltip(tooltip));
    }

    private void updateTooltipContent(View tooltip) {
        TextView tvStep = tooltip.findViewById(R.id.coach_step_indicator);
        tvStep.setText(getResources().getString(R.string.coach_step_format,
                currentStepIndex + 1, steps.size()));

        TextView tvTitle = tooltip.findViewById(R.id.coach_title);
        tvTitle.setText(currentStep.titleResId);

        TextView tvDesc = tooltip.findViewById(R.id.coach_desc);
        tvDesc.setText(currentStep.descResId);

        MaterialButton btnSkip = tooltip.findViewById(R.id.coach_btn_skip);
        btnSkip.setOnClickListener(v -> dismiss());

        MaterialButton btnNext = tooltip.findViewById(R.id.coach_btn_next);
        if (currentStep.isLastStep) {
            btnNext.setText("完成");
        } else {
            btnNext.setText("下一步");
        }
        btnNext.setOnClickListener(v -> advanceOrDismiss());
    }

    private void positionTooltip(View tooltip) {
        tooltip.measure(
                View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        int tooltipW = tooltip.getMeasuredWidth();
        int tooltipH = tooltip.getMeasuredHeight();
        int margin = DisplayUtils.dpToPx(16);
        int screenW = getWidth();
        int screenH = getHeight();

        if (currentStep.targetViewId == 0 || targetView == null) {
            tooltip.setX((screenW - tooltipW) / 2);
            tooltip.setY((screenH - tooltipH) / 2);
            return;
        }

        Rect hole = getHoleRect();
        if (hole == null) {
            tooltip.setX((screenW - tooltipW) / 2);
            tooltip.setY((screenH - tooltipH) / 2);
            return;
        }

        // X: always centered on screen
        int tooltipX = (screenW - tooltipW) / 2;

        // Y: below hole, or above if not enough space
        int tooltipY = hole.bottom + DisplayUtils.dpToPx(12);
        if (tooltipY + tooltipH > screenH) {
            tooltipY = hole.top - tooltipH - DisplayUtils.dpToPx(12);
        }
        if (tooltipY < margin) {
            tooltipY = margin;
        }

        tooltip.setX(tooltipX);
        tooltip.setY(tooltipY);
    }

    private void advanceOrDismiss() {
        if (currentStep == null) return;

        if (currentStep.isLastStep || currentStepIndex >= steps.size() - 1) {
            dismiss();
        } else {
            setAlpha(0.7f);
            animate().alpha(1f).setDuration(200).start();
            showStep(currentStepIndex + 1);
        }
    }

    public void dismiss() {
        if (dismissing) return;
        dismissing = true;

        animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    ViewGroup parent = (ViewGroup) getParent();
                    if (parent != null) parent.removeView(this);
                    if (onDismiss != null) onDismiss.run();
                })
                .start();
    }

}
