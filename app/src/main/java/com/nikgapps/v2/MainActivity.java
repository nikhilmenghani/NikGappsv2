package com.nikgapps.v2;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView title = new TextView(this);
        title.setText("NikGappsv2");
        title.setGravity(Gravity.CENTER);
        setContentView(title);
    }
}
