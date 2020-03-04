package com.mobidevelop.spl.demo

import android.app.Activity
import android.os.Bundle
import com.mobidevelop.spl.widget.SplitPaneLayout
import com.mobidevelop.spl.widget.SplitPaneLayout.OnSplitterPositionChangedListener
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DecimalFormat
import java.util.*

class MainActivity : Activity(), OnSplitterPositionChangedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        layout.onSplitterPositionChangedListener = this
    }

    override fun onResume() {
        super.onResume()
        updateViews()
    }

    override fun onSplitterPositionChanged(splitPaneLayout: SplitPaneLayout, fromUser: Boolean) {
        updateViews()
    }

    private fun updateViews() {
        val percent = DecimalFormat.getPercentInstance(Locale.getDefault())
        first.text = percent.format(layout.splitterPositionPercent.toDouble())
        second.text = percent.format(1f - layout.splitterPositionPercent.toDouble())
    }
}