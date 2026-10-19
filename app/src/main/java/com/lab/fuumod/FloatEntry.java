package com.lab.fuumod;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/** 悬浮的「其他」入口，挂在目标 App 窗口上。 */
public final class FloatEntry {

    private static TextView view;

    @SuppressLint("ClickableViewAccessibility")
    public static void show(final Activity act, final View.OnClickListener onClick) {
        if (view != null) return;
        final WindowManager wm = act.getWindowManager();
        if (wm == null) return;

        final TextView tv = new TextView(act);
        tv.setText("其他");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xCC3A7AFE);
        bg.setCornerRadius(999);
        tv.setBackground(bg);

        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                140, 140,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        lp.x = 12;

        tv.setOnClickListener(onClick);
        tv.setOnTouchListener(new View.OnTouchListener() {
            private int downX, downY;
            private float startX, startY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = (int) e.getRawX();
                        downY = (int) e.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) e.getRawX() - downX;
                        int dy = (int) e.getRawY() - downY;
                        lp.x = (int) startX - dx;
                        lp.y = (int) startY + dy;
                        try {
                            wm.updateViewLayout(tv, lp);
                        } catch (Throwable ignored) {
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        try {
            wm.addView(tv, lp);
            view = tv;
        } catch (Throwable ignored) {
            view = null;
        }
    }
}