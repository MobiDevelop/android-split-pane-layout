package com.mobidevelop.spl.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.mobidevelop.spl.widget.SplitPaneLayout;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView first;
    private TextView second;
    private SplitPaneLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        first = findViewById(R.id.first);
        second = findViewById(R.id.second);

        layout = findViewById(R.id.layout);
        layout.setOnSplitterPositionChangedListener(new SplitPaneLayout.OnSplitterPositionChangedListener() {
            @Override
            public void onSplitterPositionChanged(SplitPaneLayout splitPaneLayout, boolean fromUser) {
                updateViews();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViews();
    }

    private void updateViews() {
        NumberFormat percent = DecimalFormat.getPercentInstance(Locale.getDefault());

        first.setText(percent.format(layout.getSplitterPositionPercent()));
        second.setText(percent.format(1f - layout.getSplitterPositionPercent()));
    }
}
