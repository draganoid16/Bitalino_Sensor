package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SemiCircularProgressBar extends View {

    private Paint backgroundPaint;
    private Paint progressPaint;
    private int progress = 0;
    private int max = 100;
    private float strokeWidth = 50f;

    public SemiCircularProgressBar(Context context) {
        super(context);
        init();
    }

    public SemiCircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SemiCircularProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.BLACK);
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(Color.RED);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Determine dimensions
        int width = getWidth();
        int height = getHeight();
        // For a semi-circle, width can be larger than height; we use height * 2 as the diameter.
        int diameter = height * 2;
        float padding = strokeWidth / 2;

        // Define the oval for the arc.
        // We center the arc horizontally relative to our view.
        float left = (width - diameter) / 2f + padding;
        float top = padding;
        float right = left + diameter - 2 * padding;
        float bottom = top + diameter - 2 * padding;
        RectF rect = new RectF(left, top, right, bottom);

        // The arc covers the upper half of the circle.
        // Start at 180° (left-most point) and sweep 180° to the right-most point.
        float startAngle = 180f;
        float sweepAngle = 180f;

        // Draw the full background arc (always black)
        canvas.drawArc(rect, startAngle, sweepAngle, false, backgroundPaint);

        // Calculate the sweep for the progress (red)
        float progressSweep = (progress / (float) max) * sweepAngle;
        canvas.drawArc(rect, startAngle, progressSweep, false, progressPaint);
    }

    // Setter for progress – call invalidate() to redraw.
    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }

    public int getProgress() {
        return progress;
    }

    // Optional: setter for max
    public void setMax(int max) {
        this.max = max;
        invalidate();
    }
}
