package com.nikgapps.v2;

import android.annotation.TargetApi;
import android.app.Activity;
import androidx.activity.ComponentActivity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Picture;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public final class MainActivity extends ComponentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(Color.BLACK);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.nikgapps_logo);
        logo.setContentDescription("NikGappsv2");
        int fallbackSize = Math.round(160 * getResources().getDisplayMetrics().density);
        content.addView(logo, new FrameLayout.LayoutParams(fallbackSize, fallbackSize, Gravity.CENTER));
        StartupState startup = new StartupState();
        startup.setReady(savedInstanceState != null);
        NavigationContent.installNavigation(this, content, startup);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PlatformSplash.matchSystemIcon(this, content, logo, () -> startup.setReady(true));
        } else {
            content.postOnAnimation(() -> startup.setReady(true));
        }
    }

    @TargetApi(Build.VERSION_CODES.S)
    private static final class PlatformSplash {
        static void matchSystemIcon(Activity activity, FrameLayout content, ImageView logo, Runnable onReady) {
            activity.getSplashScreen().setOnExitAnimationListener(splash -> {
                View icon = splash.getIconView();
                if (icon != null && icon.getWidth() > 0 && icon.getHeight() > 0 && content.isAttachedToWindow()) {
                    Picture picture = new Picture();
                    try {
                        icon.draw(picture.beginRecording(icon.getWidth(), icon.getHeight()));
                    } finally {
                        picture.endRecording();
                    }
                    Bitmap capturedIcon = Bitmap.createBitmap(picture);
                    int[] iconPosition = new int[2];
                    int[] contentPosition = new int[2];
                    icon.getLocationOnScreen(iconPosition);
                    content.getLocationOnScreen(contentPosition);
                    FrameLayout.LayoutParams bounds = new FrameLayout.LayoutParams(
                            icon.getWidth(), icon.getHeight(), Gravity.TOP | Gravity.LEFT);
                    bounds.leftMargin = iconPosition[0] - contentPosition[0];
                    bounds.topMargin = iconPosition[1] - contentPosition[1];
                    logo.setImageBitmap(capturedIcon);
                    logo.setLayoutParams(bounds);
                    android.util.Log.i("NikGappsv2Splash", "Captured system icon "
                            + icon.getWidth() + "x" + icon.getHeight());
                }
                // Allow the captured icon to draw before removing the platform overlay.
                View decor = activity.getWindow().getDecorView();
                decor.postOnAnimation(() -> decor.postOnAnimation(() -> {
                    splash.remove();
                    onReady.run();
                }));
            });
        }
    }
}
