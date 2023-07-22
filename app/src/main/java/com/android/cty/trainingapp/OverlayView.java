package com.android.cty.trainingapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;


import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class OverlayView extends View {

    private Paint paint;
    private List<PoseLandmark> poseLandmarks;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8f);
    }

    public void setPoseLandmarks(List<PoseLandmark> landmarks) {
        poseLandmarks = landmarks;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (poseLandmarks != null) {
            for (PoseLandmark landmark : poseLandmarks) {
                float x = translateX(landmark.getPosition().x);
                float y = translateY(landmark.getPosition().y);
                canvas.drawCircle(x, y, 10, paint);
            }
            // 绘制骨架连接线
            drawLine(canvas, PoseLandmark.NOSE, PoseLandmark.LEFT_EYE_INNER);
            drawLine(canvas, PoseLandmark.LEFT_EYE_INNER, PoseLandmark.LEFT_EYE);
            drawLine(canvas, PoseLandmark.LEFT_EYE, PoseLandmark.LEFT_EYE_OUTER);
            // 绘制左手臂连接线
            drawLine(canvas, PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW);
            drawLine(canvas, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST);

        }
    }

    private void drawLine(Canvas canvas, int startLandmark, int endLandmark) {
        if (poseLandmarks == null) {
            return;
        }
        PoseLandmark start = poseLandmarks.get(startLandmark);
        PoseLandmark end = poseLandmarks.get(endLandmark);

        float startX = translateX(start.getPosition().x);
        float startY = translateY(start.getPosition().y);
        float endX = translateX(end.getPosition().x);
        float endY = translateY(end.getPosition().y);

        canvas.drawLine(startX, startY, endX, endY, paint);
    }
    private float translateX(float x) {
        return getWidth() - x * getWidth();
    }

    private float translateY(float y) {
        return y * getHeight();
    }




}

