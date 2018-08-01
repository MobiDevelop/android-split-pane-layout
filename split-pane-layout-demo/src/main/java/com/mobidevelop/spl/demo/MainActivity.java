package com.mobidevelop.spl.demo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.mobidevelop.spl.widget.SplitPaneLayout;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView first = findViewById(R.id.first);
        final TextView second = findViewById(R.id.second);

        final SplitPaneLayout layout = findViewById(R.id.layout);
        layout.setOnSplitterPositionChangedListener(new SplitPaneLayout.OnSplitterPositionChangedListener() {
            @Override
            public void onSplitterPositionChanged(SplitPaneLayout splitPaneLayout, boolean fromUser) {
                NumberFormat percent = DecimalFormat.getPercentInstance(Locale.getDefault());

                first.setText(percent.format(layout.getSplitterPositionPercent()));
                second.setText(percent.format(1f - layout.getSplitterPositionPercent()));
            }
        });
        layout.post(new Runnable() {
            @Override
            public void run() {
                NumberFormat percent = DecimalFormat.getPercentInstance(Locale.getDefault());

                first.setText(percent.format(layout.getSplitterPositionPercent()));
                second.setText(percent.format(1f - layout.getSplitterPositionPercent()));

            }
        });

    }

}
